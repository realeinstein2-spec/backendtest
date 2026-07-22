package com.makershub.orderpayment.dto;

import com.makershub.orderpayment.enums.DisputeStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

public final class DisputeDto {

    private DisputeDto() {}

    @Data
    public static class RaiseDisputeRequest {
        private UUID orderId;
        private String reason;
    }

    @Data
    @Builder
    public static class DisputeDetailResponse {
        private UUID id;
        private UUID orderId;
        private UUID raisedByUserId;
        private String reason;
        private DisputeStatus status;
        private Instant createdAt;
    }
}
