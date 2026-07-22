package com.makershub.marketplace.dto;

import com.makershub.marketplace.enums.BidStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class BidDto {

    private BidDto() {}

    @Data
    public static class CreateBidRequest {
        private UUID jobId;
        private BigDecimal proposedPrice;
        private Integer estimatedDays;
        private String proposalNotes;
    }

    @Data
    @Builder
    public static class BidDetailResponse {
        private UUID id;
        private UUID jobId;
        private UUID factoryOwnerId;
        private BigDecimal proposedPrice;
        private Integer estimatedDays;
        private String proposalNotes;
        private BidStatus status;
        private Instant createdAt;
    }
}
