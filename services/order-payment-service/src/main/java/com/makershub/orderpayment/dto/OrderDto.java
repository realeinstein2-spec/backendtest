package com.makershub.orderpayment.dto;

import com.makershub.orderpayment.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class OrderDto {

    private OrderDto() {}

    @Data
    public static class CreateOrderRequest {
        private UUID jobId;
        private UUID bidId;
        private UUID factoryOwnerId;
        private BigDecimal totalAmount;
    }

    @Data
    @Builder
    public static class OrderDetailResponse {
        private UUID id;
        private UUID jobId;
        private UUID bidId;
        private UUID smeOwnerId;
        private UUID factoryOwnerId;
        private BigDecimal totalAmount;
        private OrderStatus status;
        private Instant createdAt;
    }
}
