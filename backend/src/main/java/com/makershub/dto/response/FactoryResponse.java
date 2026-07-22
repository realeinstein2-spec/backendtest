package com.makershub.dto.response;

import com.makershub.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FactoryResponse {

    private FactoryResponse() {}

    @Data
    @Builder
    public static class FactoryDetailResponse {
        private UUID id;
        private UUID ownerId;
        private String ownerName;
        private String ownerPhoneNumber;
        private String companyName;
        private String description;
        private List<String> sectorTags;
        private String machineryList;
        private String payoutAccountType;
        private String payoutAccountName;
        private String payoutAccountNumber;
        private String payoutBankCode;
        private Integer minOrderQuantity;
        private Integer maxOrderQuantity;
        private Double latitude;
        private Double longitude;
        private String address;
        private VerificationStatus verificationStatus;
        private String verificationNotes;
        private Boolean isFeatured;
        private Instant featuredUntil;
        private Double responseTimeHours;
        private Double completionRate;
        private Instant createdAt;
        private Instant updatedAt;
    }

    /**
     * Public-facing factory profile combining factory details with public owner information.
     * Excludes sensitive fields like payout details, Ghana card number, etc.
     */
    @Data
    @Builder
    public static class FactoryPublicProfileResponse {
        // Factory info
        private UUID factoryId;
        private String companyName;
        private String description;
        private List<String> sectorTags;
        private String machineryList;
        private Integer minOrderQuantity;
        private Integer maxOrderQuantity;
        private Double latitude;
        private Double longitude;
        private String address;
        private VerificationStatus verificationStatus;
        private Boolean isFeatured;
        private Double responseTimeHours;
        private Double completionRate;
        private Instant factoryCreatedAt;

        // Owner (manufacturer) public info
        private UUID ownerId;
        private String ownerName;
        private String ownerPhoneNumber;
        private String ownerEmail;
        private String ownerRegion;
        private String ownerTown;
        private String profileImageUrl;
        private String coverImageUrl;
        private Double ratingAvg;
        private Integer reviewCount;
        private Integer totalOrders;
        private Instant memberSince;
    }
}
