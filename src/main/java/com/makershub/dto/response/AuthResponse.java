package com.makershub.dto.response;

import com.makershub.enums.UserRole;
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
        private String region;
        private String profileImageUrl;
        private String otpCode;
    }
}
