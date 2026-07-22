package com.makershub.auth.dto;

import com.makershub.auth.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

public final class AuthResponse {

    private AuthResponse() {}

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

    @Data
    @Builder
    public static class PendingAuthResponse {
        private String phoneNumber;
        private String message;
        private String otpCode;
    }

    @Data
    @Builder
    public static class UserSummaryResponse {
        private UUID id;
        private String phoneNumber;
        private String fullName;
        private UserRole role;
        private Boolean isVerified;
        private Boolean isActive;
        private String email;
        private String ghanaCardNumber;
        private String region;
        private String town;
        private String profileImageUrl;
        private String coverImageUrl;
        private Instant lastActiveAt;
        private Instant createdAt;
        private Instant updatedAt;
        private String otpCode;
    }
}
