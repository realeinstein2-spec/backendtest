package com.makershub.dto.response;

import com.makershub.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

public final class AuthResponse {

    private AuthResponse() {}

    /**
     * C-8: Returned after successful OTP verification.
     * Contains the JWT access + refresh tokens.
     * otpCode field removed — tokens are only issued post-OTP-verification.
     */
    @Data
    @Builder
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private Instant accessTokenExpiry;
        private Instant refreshTokenExpiry;
        private String tokenType;
        private UserSummaryResponse user;
    }

    /**
     * C-8: Returned from login(). Signals that credentials are valid and OTP was sent.
     * No JWT tokens are included — client must call POST /auth/verify to get tokens.
     */
    @Data
    @Builder
    public static class PendingAuthResponse {
        private String phoneNumber;
        private String message;
        /** Only populated in dev profile for Swagger testing convenience. Null in production. */
        private String otpCode;
    }

    /**
     * Returned from register(). Confirms account was created and OTP was dispatched.
     */
    @Data
    @Builder
    public static class UserSummaryResponse {
        private UUID id;
        private String phoneNumber;
        private String fullName;
        private UserRole role;
        private Boolean isVerified;
        private String email;
        private String ghanaCardNumber;
        private String region;
        private String town;
        private String profileImageUrl;
        private Instant lastActiveAt;
        private Instant createdAt;
        private Instant updatedAt;
        /** Only populated in dev profile for Swagger testing convenience. Null in production. */
        private String otpCode;
    }
}
