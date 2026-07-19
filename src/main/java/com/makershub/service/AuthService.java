package com.makershub.service;

import com.makershub.dto.request.AuthRequest;
import com.makershub.dto.response.AuthResponse;
import com.makershub.entity.OtpVerification;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
import com.makershub.enums.UserRole;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.audit.AuditLogger;
import com.makershub.notification.NotificationEvent;
import com.makershub.notification.SmsService;
import com.makershub.repository.OtpVerificationRepository;
import com.makershub.repository.UserRepository;
import com.makershub.security.JwtUtil;
import com.makershub.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Collections;
import java.util.Base64;
import java.util.Map;
import java.util.List;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Cryptographically secure RNG for OTP generation (C-5). */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Maximum failed OTP attempts before lockout (C-7). */
    private static final int MAX_OTP_ATTEMPTS = 5;

    /** Roles that users are allowed to self-register as (C-1). ADMIN is intentionally excluded. */
    private static final Set<UserRole> SELF_REGISTRABLE_ROLES =
            Set.of(UserRole.SME_OWNER, UserRole.FACTORY_OWNER, UserRole.ENTERPRISE);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuditLogger auditLogger;
    private final OtpVerificationRepository otpVerificationRepository;
    private final SmsService smsService;
    private final Environment environment;

    // ─────────────────────────────── REGISTER ────────────────────────────────

    @Transactional
    public AuthResponse.UserSummaryResponse register(AuthRequest.RegisterRequest request) {
        // C-1: Whitelist allowed self-registration roles — ADMIN is blocked
        UserRole requestedRole = request.getRole() != null ? request.getRole() : UserRole.SME_OWNER;
        if (!SELF_REGISTRABLE_ROLES.contains(requestedRole)) {
            throw new BusinessException(
                    "Invalid role for self-registration. Allowed: SME_OWNER, FACTORY_OWNER, ENTERPRISE.",
                    HttpStatus.BAD_REQUEST, "INVALID_ROLE");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("Phone number already registered", HttpStatus.CONFLICT, "PHONE_EXISTS");
        }

        User user = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(requestedRole)
                .ghanaCardNumber(request.getGhanaCardNumber())
                .region(request.getRegion())
                .town(request.getTown())
                .isVerified(false)
                .isActive(true)
                .ratingAvg(0.0)
                .totalOrders(0)
                .build();

        User saved = userRepository.save(user);
        auditLogger.log(AuditAction.CREATE, "USER", saved.getId(), null, null);

        String otpCode = generateAndSaveOtp(saved.getPhoneNumber(), saved.getId());

        // C-8: OTP is NEVER exposed in HTTP response in production.
        // In dev profile only, otpCode is returned for testing convenience.
        String responseOtp = isDevProfile() ? otpCode : null;

        return mapToSummary(saved, responseOtp);
    }

    // ──────────────────────────────── LOGIN ──────────────────────────────────

    /**
     * C-8: login() now returns a PendingAuthResponse — NO JWT tokens yet.
     * Tokens are only issued from verifyOtp() after OTP is confirmed.
     */
    @Transactional
    public AuthResponse.PendingAuthResponse login(AuthRequest.LoginRequest request) {
        // C-9: Fetch user first so we can check account status before auth
        User user = userRepository.findByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException("Invalid credentials",
                        HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        // C-9: Reject suspended accounts before wasting authentication effort
        if (!user.getIsActive()) {
            throw new BusinessException("Account is suspended. Contact support.",
                    HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BusinessException("Invalid credentials", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        String otpCode = generateAndSaveOtp(user.getPhoneNumber(), user.getId());

        // C-8: Return pending auth — no tokens until OTP verified
        String responseOtp = isDevProfile() ? otpCode : null;
        return AuthResponse.PendingAuthResponse.builder()
                .phoneNumber(user.getPhoneNumber())
                .message("OTP sent to your registered phone. Please verify to complete login.")
                .otpCode(responseOtp)
                .build();
    }

    // ───────────────────────────── VERIFY OTP ────────────────────────────────

    /**
     * C-7 + C-8: Verifies OTP with attempt counting. JWT tokens are ONLY issued here.
     */
    @Transactional
    public AuthResponse.TokenResponse verifyOtp(AuthRequest.OtpRequest request) {
        OtpVerification verification = otpVerificationRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException(
                        "No OTP found for this phone number. Please login again.",
                        HttpStatus.BAD_REQUEST, "OTP_NOT_FOUND"));

        // C-7: Enforce max attempt limit before checking anything else
        if (verification.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
            otpVerificationRepository.delete(verification);
            throw new BusinessException(
                    "Too many failed attempts. Please login again to receive a new OTP.",
                    HttpStatus.TOO_MANY_REQUESTS, "OTP_MAX_ATTEMPTS");
        }

        if (verification.getExpiryTime().isBefore(java.time.Instant.now())) {
            otpVerificationRepository.delete(verification);
            throw new BusinessException("OTP has expired. Please login again.",
                    HttpStatus.BAD_REQUEST, "OTP_EXPIRED");
        }

        if (!verification.getOtpCode().equals(request.getOtp())) {
            // C-7: Increment attempt count on each wrong guess
            verification.setAttemptCount(verification.getAttemptCount() + 1);
            otpVerificationRepository.save(verification);
            int remaining = MAX_OTP_ATTEMPTS - verification.getAttemptCount();
            throw new BusinessException(
                    "Invalid OTP code. " + remaining + " attempt(s) remaining.",
                    HttpStatus.BAD_REQUEST, "INVALID_OTP");
        }

        // OTP valid — mark user as verified and clean up
        User user = userRepository.findByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getPhoneNumber()));
        user.setIsVerified(true);
        userRepository.save(user);

        otpVerificationRepository.delete(verification);
        auditLogger.log(AuditAction.VERIFY, "USER", user.getId(), "UNVERIFIED", "VERIFIED");

        // C-8: Issue JWT tokens ONLY after successful OTP verification
        return buildTokenResponse(new UserDetailsImpl(user), user);
    }

    // ───────────────────────────── REFRESH TOKEN ─────────────────────────────

    @Transactional
    public AuthResponse.TokenResponse refresh(AuthRequest.RefreshRequest request) {
        if (!jwtUtil.isTokenValid(request.getRefreshToken(), "REFRESH")) {
            throw new BusinessException("Invalid refresh token", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }
        UUID userId = jwtUtil.extractUserId(request.getRefreshToken());
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        // H-2/H-3: Re-check account status on every token refresh
        if (!user.getIsActive()) {
            throw new BusinessException("Account is suspended", HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED");
        }
        if (!user.getIsVerified()) {
            throw new BusinessException("Account is not verified", HttpStatus.FORBIDDEN, "ACCOUNT_NOT_VERIFIED");
        }
        return buildTokenResponse(new UserDetailsImpl(user), user);
    }

    // ─────────────────────────────── HELPERS ─────────────────────────────────

    /**
     * C-5: Generate a cryptographically secure 6-digit OTP via SecureRandom.
     * C-6: OTP is dispatched via SMS only — never logged at INFO/WARN level.
     */
    private String generateAndSaveOtp(String phoneNumber, UUID userId) {
        String otpCode = String.format("%04d", SECURE_RANDOM.nextInt(10_000));

        OtpVerification verification = otpVerificationRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> OtpVerification.builder().phoneNumber(phoneNumber).build());
        verification.setOtpCode(otpCode);
        verification.setExpiryTime(java.time.Instant.now().plusSeconds(300)); // 5 min
        verification.setAttemptCount(0); // reset attempts on fresh OTP
        otpVerificationRepository.save(verification);

        // C-6: Only log masked phone at DEBUG — no OTP value in log output
        log.debug("[OTP] Dispatched for ...{}", maskPhone(phoneNumber));

        NotificationEvent event = NotificationEvent.builder()
                .recipientId(userId)
                .phoneNumber(phoneNumber)
                .body("Your MakersHub code is " + otpCode + ". Expires in 5 minutes. Do not share.")
                .title("MakersHub Verification")
                .build();
        smsService.send(event);

        return otpCode;
    }

    private AuthResponse.TokenResponse buildTokenResponse(UserDetailsImpl userDetails, User user) {
        AuthResponse.UserSummaryResponse userSummary = mapToSummary(user, null);

        return AuthResponse.TokenResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(userDetails))
                .refreshToken(jwtUtil.generateRefreshToken(userDetails))
                .accessTokenExpiry(jwtUtil.getAccessExpiry())
                .refreshTokenExpiry(jwtUtil.getRefreshExpiry())
                .tokenType("Bearer")
                .user(userSummary)
                .build();
    }

    private AuthResponse.UserSummaryResponse mapToSummary(User user, String otpCode) {
        String payoutType = null;
        String payoutName = null;
        String payoutNumber = null;
        String payoutBank = null;
        if (user.getFactory() != null) {
            payoutType = user.getFactory().getPayoutAccountType();
            payoutName = user.getFactory().getPayoutAccountName();
            payoutNumber = user.getFactory().getPayoutAccountNumber();
            payoutBank = user.getFactory().getPayoutBankCode();
        }
        return AuthResponse.UserSummaryResponse.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .email(user.getEmail())
                .ghanaCardNumber(user.getGhanaCardNumber())
                .region(user.getRegion())
                .town(user.getTown())
                .profileImageUrl(user.getProfileImageUrl())
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .otpCode(otpCode)
                .isActive(user.getIsActive())
                .payoutAccountType(payoutType)
                .payoutAccountName(payoutName)
                .payoutAccountNumber(payoutNumber)
                .payoutBankCode(payoutBank)
                .build();
    }

    @Transactional
    public AuthResponse.TokenResponse socialLogin(AuthRequest.SocialLoginRequest request, String provider) {
        String email = null;
        String fullName = request.getFullName();

        if ("google".equalsIgnoreCase(provider)) {
            try {
                NetHttpTransport transport = new NetHttpTransport();
                GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
                String googleClientId = environment.getProperty("makershub.google.client-id");
                
                GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                        .setAudience(Collections.singletonList(googleClientId))
                        .build();

                GoogleIdToken idToken = verifier.verify(request.getIdToken());
                if (idToken == null) {
                    throw new BusinessException("Invalid Google token", HttpStatus.UNAUTHORIZED, "INVALID_SOCIAL_TOKEN");
                }
                GoogleIdToken.Payload payload = idToken.getPayload();
                email = payload.getEmail();
                if (fullName == null || fullName.trim().isEmpty()) {
                    fullName = (String) payload.get("name");
                }
            } catch (Exception e) {
                throw new BusinessException("Google verification failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED, "SOCIAL_AUTH_FAILED");
            }
        } else if ("apple".equalsIgnoreCase(provider)) {
            try {
                String appleClientId = environment.getProperty("makershub.apple.client-id");
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                Map<String, Object> response = restTemplate.getForObject("https://appleid.apple.com/auth/keys", Map.class);
                List<Map<String, Object>> keys = (List<Map<String, Object>>) response.get("keys");
                
                String kid = null;
                String[] parts = request.getIdToken().split("\\.");
                if (parts.length > 0) {
                    String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
                    Map<String, Object> header = new com.fasterxml.jackson.databind.ObjectMapper().readValue(headerJson, Map.class);
                    kid = (String) header.get("kid");
                }
                
                if (kid == null) {
                    throw new BusinessException("Missing key ID in Apple token", HttpStatus.UNAUTHORIZED, "INVALID_SOCIAL_TOKEN");
                }
                
                Map<String, Object> matchingKey = null;
                for (Map<String, Object> key : keys) {
                    if (kid.equals(key.get("kid"))) {
                        matchingKey = key;
                        break;
                    }
                }
                if (matchingKey == null) {
                    throw new BusinessException("No matching Apple key found", HttpStatus.UNAUTHORIZED, "INVALID_SOCIAL_TOKEN");
                }
                
                String modulus = (String) matchingKey.get("n");
                String exponent = (String) matchingKey.get("e");
                byte[] nb = Base64.getUrlDecoder().decode(modulus);
                byte[] eb = Base64.getUrlDecoder().decode(exponent);
                java.math.BigInteger n = new java.math.BigInteger(1, nb);
                java.math.BigInteger e = new java.math.BigInteger(1, eb);
                java.security.spec.RSAPublicKeySpec spec = new java.security.spec.RSAPublicKeySpec(n, e);
                java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
                java.security.PublicKey publicKey = kf.generatePublic(spec);
                
                Claims claims = Jwts.parser()
                        .verifyWith(publicKey)
                        .build()
                        .parseSignedClaims(request.getIdToken())
                        .getPayload();
                
                if (!"https://appleid.apple.com".equals(claims.getIssuer())) {
                    throw new BusinessException("Invalid Apple issuer", HttpStatus.UNAUTHORIZED, "INVALID_SOCIAL_TOKEN");
                }
                if (appleClientId != null && !claims.getAudience().contains(appleClientId)) {
                    throw new BusinessException("Invalid Apple audience", HttpStatus.UNAUTHORIZED, "INVALID_SOCIAL_TOKEN");
                }
                
                email = claims.get("email", String.class);
            } catch (Exception e) {
                throw new BusinessException("Apple verification failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED, "SOCIAL_AUTH_FAILED");
            }
        } else {
            throw new BusinessException("Unsupported social provider: " + provider, HttpStatus.BAD_REQUEST, "UNSUPPORTED_PROVIDER");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException("Email address is required from social provider", HttpStatus.BAD_REQUEST, "MISSING_EMAIL");
        }

        final String finalEmail = email;
        final String finalFullName = (fullName != null && !fullName.trim().isEmpty()) ? fullName : email.split("@")[0];

        User user = userRepository.findByEmailAndDeletedAtIsNull(finalEmail)
                .orElseGet(() -> {
                    String phone = request.getPhoneNumber();
                    if (phone == null || phone.trim().isEmpty()) {
                        throw new BusinessException("Phone number is required for first-time registration", HttpStatus.BAD_REQUEST, "PHONE_REQUIRED");
                    }
                    if (userRepository.existsByPhoneNumber(phone)) {
                        throw new BusinessException("Phone number already registered", HttpStatus.CONFLICT, "PHONE_EXISTS");
                    }
                    if (!SELF_REGISTRABLE_ROLES.contains(request.getRole())) {
                        throw new BusinessException("Role not allowed", HttpStatus.BAD_REQUEST, "ROLE_NOT_ALLOWED");
                    }

                    User newUser = User.builder()
                            .phoneNumber(phone)
                            .email(finalEmail)
                            .fullName(finalFullName)
                            .role(request.getRole())
                            .isVerified(true)
                            .isActive(true)
                            .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .build();

                    User saved = userRepository.save(newUser);
                    auditLogger.log(AuditAction.CREATE, "USER", saved.getId(), null, request.getRole().name());
                    return saved;
                });

        if (!user.getIsActive()) {
            throw new BusinessException("Account is suspended", HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED");
        }

        return buildTokenResponse(new UserDetailsImpl(user), user);
    }

    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(phone.length() - 4);
    }
}
