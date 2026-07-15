package com.makershub.dto.response;

import com.makershub.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

public final class AuthResponse {

    private AuthResponse() {}

    /** Returned after successful OTP verification - contains the JWT tokens */
    @Data
    @Builder
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private Instant accessTokenExpiry;
        private Instant refreshTokenExpiry;
        private String tokenType;
        // otpCode removed - tokens are only issued after OTP verification
    }

    /** Returned after login() - signals OTP was sent, no tokens yet (C-8: MFA fix) */
    @Data
    @Builder
    public static class PendingAuthResponse {
        private String phoneNumber;
        private String message;
        /** Only populated in dev profile for testing convenience */
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
        /** Only populated in dev profile for testing convenience */
        private String otpCode;
    }
}
