package com.makershub.dto.request;

import com.makershub.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class OrderRequest {

    private OrderRequest() {}

    @Data
    public static class StatusUpdateRequest {
        @NotNull
        private OrderStatus newStatus;

        @Size(max = 1000)
        private String notes;
    }

    @Data
    public static class ConfirmDeliveryRequest {
        @NotNull
        private Boolean qualityAccepted;

        @Size(max = 2000)
        private String comment;
    }
}
