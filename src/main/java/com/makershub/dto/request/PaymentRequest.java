package com.makershub.dto.request;

import com.makershub.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public final class PaymentRequest {

    private PaymentRequest() {}

    @Data
    public static class InitiatePaymentRequest {
        @NotBlank
        private String orderId;

        @NotNull
        private PaymentMethod paymentMethod;
    }
}
