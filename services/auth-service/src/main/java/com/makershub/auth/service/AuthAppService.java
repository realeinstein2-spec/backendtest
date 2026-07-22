package com.makershub.auth.service;

import com.makershub.auth.dto.AuthRequest;
import com.makershub.auth.dto.AuthResponse;
import com.makershub.auth.entity.OtpVerification;
import com.makershub.auth.entity.User;
import com.makershub.auth.enums.UserRole;
import com.makershub.auth.repository.OtpVerificationRepository;
import com.makershub.auth.repository.UserRepository;
import com.makershub.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAppService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final Set<UserRole> SELF_REGISTRABLE_ROLES =
            Set.of(UserRole.SME_OWNER, UserRole.FACTORY_OWNER, UserRole.ENTERPRISE);

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse.UserSummaryResponse register(AuthRequest.RegisterRequest request) {
        UserRole requestedRole = request.getRole() != null ? request.getRole() : UserRole.SME_OWNER;
        if (!SELF_REGISTRABLE_ROLES.contains(requestedRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role for self-registration");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already registered");
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
        String otpCode = generateAndSaveOtp(saved.getPhoneNumber());

        return mapToSummary(saved, otpCode);
    }

    @Transactional
    public AuthResponse.PendingAuthResponse login(AuthRequest.LoginRequest request) {
        User user = userRepository.findByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is suspended");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String otpCode = generateAndSaveOtp(user.getPhoneNumber());

        return AuthResponse.PendingAuthResponse.builder()
                .phoneNumber(user.getPhoneNumber())
                .message("OTP sent to your registered phone. Please verify to complete login.")
                .otpCode(otpCode)
                .build();
    }

    @Transactional
    public AuthResponse.PendingAuthResponse resendOtp(AuthRequest.ResendOtpRequest request) {
        User user = userRepository.findByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is suspended");
        }

        String otpCode = generateAndSaveOtp(user.getPhoneNumber());

        return AuthResponse.PendingAuthResponse.builder()
                .phoneNumber(user.getPhoneNumber())
                .message("OTP resent successfully.")
                .otpCode(otpCode)
                .build();
    }

    @Transactional
    public AuthResponse.TokenResponse verifyOtp(AuthRequest.OtpRequest request) {
        OtpVerification verification = otpVerificationRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP not found"));

        if (verification.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
            otpVerificationRepository.delete(verification);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts");
        }

        if (verification.getExpiryTime().isBefore(Instant.now())) {
            otpVerificationRepository.delete(verification);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expired");
        }

        if (!verification.getOtpCode().equals(request.getOtp())) {
            verification.setAttemptCount(verification.getAttemptCount() + 1);
            otpVerificationRepository.save(verification);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP code");
        }

        User user = userRepository.findByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setIsVerified(true);
        userRepository.save(user);

        otpVerificationRepository.delete(verification);

        return buildTokenResponse(user);
    }

    @Transactional
    public AuthResponse.TokenResponse refresh(AuthRequest.RefreshRequest request) {
        if (!jwtUtil.isTokenValid(request.getRefreshToken(), "REFRESH")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        UUID userId = jwtUtil.extractUserId(request.getRefreshToken());
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is suspended");
        }
        if (!user.getIsVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not verified");
        }
        return buildTokenResponse(user);
    }

    private String generateAndSaveOtp(String phoneNumber) {
        String otpCode = String.format("%04d", SECURE_RANDOM.nextInt(10_000));

        OtpVerification verification = otpVerificationRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> OtpVerification.builder().phoneNumber(phoneNumber).build());
        verification.setOtpCode(otpCode);
        verification.setExpiryTime(Instant.now().plusSeconds(300));
        verification.setAttemptCount(0);
        otpVerificationRepository.save(verification);

        return otpCode;
    }

    private AuthResponse.TokenResponse buildTokenResponse(User user) {
        return AuthResponse.TokenResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user.getId(), user.getPhoneNumber(), user.getRole()))
                .refreshToken(jwtUtil.generateRefreshToken(user.getId()))
                .accessTokenExpiry(jwtUtil.getAccessExpiry())
                .refreshTokenExpiry(jwtUtil.getRefreshExpiry())
                .tokenType("Bearer")
                .user(mapToSummary(user, null))
                .build();
    }

    private AuthResponse.UserSummaryResponse mapToSummary(User user, String otpCode) {
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
                .coverImageUrl(user.getCoverImageUrl())
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .otpCode(otpCode)
                .isActive(user.getIsActive())
                .build();
    }
}
