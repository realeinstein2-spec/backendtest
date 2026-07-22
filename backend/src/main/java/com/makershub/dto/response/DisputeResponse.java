package com.makershub.dto.response;

import com.makershub.enums.DisputeReason;
import com.makershub.enums.DisputeStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DisputeResponse {

    private DisputeResponse() {}

    @Data
    @Builder
    public static class DisputeDetailResponse {
        private UUID id;
        private UUID orderId;
        private UUID raisedById;
        private DisputeReason reason;
        private String description;
        private List<String> evidenceUrls;
        private DisputeStatus status;
        private UUID assignedAdminId;
        private String adminNotes;
        private BigDecimal resolutionAmountGhs;
        private Instant resolvedAt;
        private Instant createdAt;
    }
}
