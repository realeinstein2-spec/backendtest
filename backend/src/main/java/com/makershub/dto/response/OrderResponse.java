package com.makershub.dto.response;

import com.makershub.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class OrderResponse {

    private OrderResponse() {}

    @Data
    @Builder
    public static class OrderDetailResponse {
        private UUID id;
        private UUID jobId;
        private UUID bidId;
        private UUID smeId;
        private UUID factoryId;
        private String factoryName;
        private String smeName;
        private BigDecimal agreedAmountGhs;
        private BigDecimal platformFeeGhs;
        private BigDecimal factoryPayoutGhs;
        private OrderStatus status;
        private Integer currentProgressPercentage;
        private String currentProductionStage;
        private Instant qualityCheckDeadline;
        private Instant deliveredAt;
        private Instant completedAt;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
