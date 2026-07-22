package com.makershub.auth.dto;

import com.makershub.auth.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class AuthRequest {

    private AuthRequest() {}

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Valid phone number required")
        private String phoneNumber;

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank
        @Size(max = 200)
        private String fullName;

        @NotNull
        private UserRole role;

        @Size(max = 50)
        private String ghanaCardNumber;

        @Size(max = 100)
        private String region;

        @Size(max = 100)
        private String town;
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String phoneNumber;

        @NotBlank
        private String password;
    }

    @Data
    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;
    }

    @Data
    public static class OtpRequest {
        @NotBlank
        private String phoneNumber;

        @NotBlank
        @Size(min = 4, max = 8)
        private String otp;
    }

    @Data
    public static class SocialLoginRequest {
        @NotBlank
        private String idToken;

        @NotNull
        private UserRole role;

        private String fullName;

        private String phoneNumber;
    }

    @Data
    public static class ResendOtpRequest {
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Valid phone number required")
        private String phoneNumber;
    }
}
