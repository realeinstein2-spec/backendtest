package com.makershub.dto.request;

import com.makershub.enums.VerificationStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

public final class FactoryRequest {

    private FactoryRequest() {}

    @Data
    public static class CreateFactoryProfileRequest {
        @NotBlank
        @Size(max = 200)
        private String companyName;

        @Size(max = 2000)
        private String description;

        @NotEmpty
        private List<@Size(max = 50) String> sectorTags;

        private String machineryList;

        private Integer minOrderQuantity;

        private Integer maxOrderQuantity;

        private Double latitude;

        private Double longitude;

        @Size(max = 500)
        private String address;

        @Size(max = 255)
        private String email;

        @Size(max = 100)
        private String region;

        @Size(max = 100)
        private String town;

        @Size(max = 500)
        private String profileImageUrl;

        @Size(max = 50)
        private String payoutAccountType;

        @Size(max = 255)
        private String payoutAccountName;

        @Size(max = 100)
        private String payoutAccountNumber;

        @Size(max = 50)
        private String payoutBankCode;
    }

    @Data
    public static class VerificationReviewRequest {
        @NotNull
        private VerificationStatus status;

        @Size(max = 1000)
        private String notes;
    }
}
