package com.makershub.dto.response;

import com.makershub.enums.BidStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class BidResponse {

    private BidResponse() {}

    @Data
    @Builder
    public static class BidDetailResponse {
        private UUID id;
        private UUID jobId;
        private UUID factoryId;
        private String factoryName;
        private List<String> factorySectorTags;
        private String factoryLogoUrl;
        private Double factoryRating;
        private Double factoryLatitude;
        private Double factoryLongitude;
        private String factoryAddress;

        // Enriched manufacturer details
        private String factoryDescription;
        private Integer factoryMinOrderQuantity;
        private Integer factoryMaxOrderQuantity;
        private Double factoryCompletionRate;
        private Double factoryResponseTimeHours;
        private Integer factoryTotalOrders;
        private String factoryCoverImageUrl;
        private String factoryRegion;
        private String factoryTown;
        private String factoryVerificationStatus;

        private BigDecimal pricePerUnitGhs;
        private BigDecimal totalPriceGhs;
        private Integer productionDays;
        private LocalDate deliveryDateEstimate;
        private String message;
        private BidStatus status;
        private Instant createdAt;
    }
}
