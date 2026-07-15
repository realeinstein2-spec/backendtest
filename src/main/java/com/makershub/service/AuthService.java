package com.makershub.service;

import com.makershub.dto.request.AuthRequest;
import com.makershub.dto.response.AuthResponse;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
import com.makershub.enums.UserRole;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.audit.AuditLogger;
import com.makershub.repository.UserRepository;
import com.makershub.security.JwtUtil;
import com.makershub.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.makershub.entity.OtpVerification;
import com.makershub.repository.OtpVerificationRepository;
import com.makershub.notification.NotificationEvent;
import com.makershub.notification.SmsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final Set<UserRole> SELF_REGISTRABLE_ROLES = Set.of(UserRole.SME_OWNER, UserRole.FACTORY_OWNER, UserRole.ENTERPRISE);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuditLogger auditLogger;
    private final OtpVerificationRepository otpVerificationRepository;
    private final SmsService smsService;
    private final Environment environment;

    @Transactional
    public AuthResponse.UserSummaryResponse register(AuthRequest.RegisterRequest request) {
        // C-1: Block self-registration as ADMIN or other privileged roles
        UserRole requestedRole = request.getRole() != null ? request.getRole() : UserRole.SME_OWNER;
        if (!SELF_REGISTRABLE_ROLES.contains(requestedRole)) {
            throw new BusinessException("Invalid role for self-registration", HttpStatus.BAD_REQUEST, "INVALID_ROLE");
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

        // C-5: Use SecureRandom for OTP generation
        String otpCode = String.format("%06d", SECURE_RANDOM.nextInt(1000000));

        OtpVerification verification = otpVerificationRepository.findByPhoneNumber(saved.getPhoneNumber())
                .orElseGet(() -> OtpVerification.builder().phoneNumber(saved.getPhoneNumber()).build());
        verification.setOtpCode(otpCode);
        verification.setExpiryTime(java.time.Instant.now().plusSeconds(300));
        verification.setAttemptCount(0);
        otpVerificationRepository.save(verification);

        // C-6: OTP is NEVER logged at INFO level in production - only dispatched via SMS
        log.debug("[OTP] Registration OTP dispatched for phone ending in ...{}",
                saved.getPhoneNumber().length() > 4 ? saved.getPhoneNumber().substring(saved.getPhoneNumber().length() - 4) : "***");

        NotificationEvent otpEvent = NotificationEvent.builder()
                .recipientId(saved.getId())
                .phoneNumber(saved.getPhoneNumber())
                .body("Your MakersHub verification code is " + otpCode + ". It expires in 5 minutes.")
                .title("MakersHub Verification")
                .build();
        smsService.send(otpEvent);

        // C-8: Do NOT return OTP in HTTP response. Register only confirms OTP was sent.
        // Dev environments can check SMS logs or enable debug logging.
        boolean isDevProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        String responseOtp = isDevProfile ? otpCode : null;

        return AuthResponse.UserSummaryResponse.builder()
                .id(saved.getId())
                .phoneNumber(saved.getPhoneNumber())
                .fullName(saved.getFullName())
                .role(saved.getRole())
                .isVerified(saved.getIsVerified())
                .region(saved.getRegion())
                .otpCode(responseOtp)
                .build();
    }

    @Transactional
    public AuthResponse.PendingAuthResponse login(AuthRequest.LoginRequest request) {
        // C-9: Check account status before returning anything
        User user = userRepository.findByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException("Invalid credentials", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        if (!user.getIsActive()) {
            throw new BusinessException("Account is suspended. Contact support.", HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BusinessException("Invalid credentials", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        // C-5: Use SecureRandom for OTP generation
        String otpCode = String.format("%06d", SECURE_RANDOM.nextInt(1000000));

        OtpVerification verification = otpVerificationRepository.findByPhoneNumber(user.getPhoneNumber())
                .orElseGet(() -> OtpVerification.builder().phoneNumber(user.getPhoneNumber()).build());
        verification.setOtpCode(otpCode);
        verification.setExpiryTime(java.time.Instant.now().plusSeconds(300));
        verification.setAttemptCount(0);
        otpVerificationRepository.save(verification);

        // C-6: OTP is NEVER logged at INFO level
        log.debug("[OTP] Login OTP dispatched for phone ending in ...{}",
                user.getPhoneNumber().length() > 4 ? user.getPhoneNumber().substring(user.getPhoneNumber().length() - 4) : "***");

        NotificationEvent otpEvent = NotificationEvent.builder()
                .recipientId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .body("Your MakersHub login verification code is " + otpCode + ". It expires in 5 minutes.")
                .title("MakersHub Login Verification")
                .build();
        smsService.send(otpEvent);

        // C-8: Return a pending auth response - NO JWT tokens until OTP is verified
        boolean isDevProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        String responseOtp = isDevProfile ? otpCode : null;
        return AuthResponse.PendingAuthResponse.builder()
                .phoneNumber(user.getPhoneNumber())
                .message("OTP sent. Please verify to complete login.")
                .otpCode(responseOtp)
                .build();
    }

    @Transactional
    public AuthResponse.TokenResponse verifyOtp(AuthRequest.OtpRequest request) {
        OtpVerification verification = otpVerificationRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException("No OTP found for this phone number", HttpStatus.BAD_REQUEST, "OTP_NOT_FOUND"));

        // C-7: Check attempt count before allowing further attempts
        if (verification.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
            otpVerificationRepository.delete(verification);
            throw new BusinessException("Too many failed attempts. Please request a new OTP.", HttpStatus.TOO_MANY_REQUESTS, "OTP_MAX_ATTEMPTS");
        }

        if (verification.getExpiryTime().isBefore(java.time.Instant.now())) {
            otpVerificationRepository.delete(verification);
            throw new BusinessException("OTP has expired", HttpStatus.BAD_REQUEST, "OTP_EXPIRED");
        }

        if (!verification.getOtpCode().equals(request.getOtp())) {
            // C-7: Increment attempt count on failure
            verification.setAttemptCount(verification.getAttemptCount() + 1);
            otpVerificationRepository.save(verification);
            int remaining = MAX_OTP_ATTEMPTS - verification.getAttemptCount();
            throw new BusinessException("Invalid OTP code. " + remaining + " attempt(s) remaining.",
                    HttpStatus.BAD_REQUEST, "INVALID_OTP");
        }

        User user = userRepository.findByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getPhoneNumber()));
        user.setIsVerified(true);
        userRepository.save(user);

        otpVerificationRepository.delete(verification);
        auditLogger.log(AuditAction.VERIFY, "USER", user.getId(), "UNVERIFIED", "VERIFIED");
        log.info("User {} verified successfully", request.getPhoneNumber().replaceAll(".(?=.{4})", "*"));

        // C-8: Issue JWT tokens ONLY after successful OTP verification
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        return buildTokenResponse(userDetails);
    }

    @Transactional
    public AuthResponse.TokenResponse refresh(AuthRequest.RefreshRequest request) {
        if (!jwtUtil.isTokenValid(request.getRefreshToken(), "REFRESH")) {
            throw new BusinessException("Invalid refresh token", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }
        UUID userId = jwtUtil.extractUserId(request.getRefreshToken());
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        // H-2/H-3: Check account is still active on refresh
        if (!user.getIsActive()) {
            throw new BusinessException("Account is suspended", HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED");
        }
        if (!user.getIsVerified()) {
            throw new BusinessException("Account is not verified", HttpStatus.FORBIDDEN, "ACCOUNT_NOT_VERIFIED");
        }
        return buildTokenResponse(new UserDetailsImpl(user));
    }

    private AuthResponse.TokenResponse buildTokenResponse(UserDetailsImpl userDetails) {
        return AuthResponse.TokenResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(userDetails))
                .refreshToken(jwtUtil.generateRefreshToken(userDetails))
                .accessTokenExpiry(jwtUtil.getAccessExpiry())
                .refreshTokenExpiry(jwtUtil.getRefreshExpiry())
                .tokenType("Bearer")
                .build();
    }
}
