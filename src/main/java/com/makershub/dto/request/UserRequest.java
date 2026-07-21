package com.makershub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class UserRequest {

    private UserRequest() {}

    @Data
    public static class UpdateProfileRequest {
        @Size(max = 200)
        private String fullName;

        @Size(max = 255)
        private String email;

        @Size(max = 100)
        private String region;

        @Size(max = 100)
        private String town;

        @Size(max = 500)
        private String profileImageUrl;

        @Size(max = 500)
        private String coverImageUrl;
    }

    @Data
    public static class UpdateFcmTokenRequest {
        @NotBlank
        private String fcmToken;
    }
}
