package com.makershub.orderpayment.dto;

import com.makershub.orderpayment.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PaymentDto {

    private PaymentDto() {}

    @Data
    public static class InitializePaymentRequest {
        private UUID orderId;
        private BigDecimal amount;
    }

    @Data
    @Builder
    public static class PaymentInitResponse {
        private String paymentReference;
        private String authorizationUrl;
        private PaymentStatus status;
    }
}
