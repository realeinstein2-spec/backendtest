package com.makershub.dto.request;

import com.makershub.enums.DisputeReason;
import com.makershub.enums.DisputeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public final class DisputeRequest {

    private DisputeRequest() {}

    @Data
    public static class CreateDisputeRequest {
        @NotNull
        private DisputeReason reason;

        @NotBlank
        @Size(max = 4000)
        private String description;

        private List<@Size(max = 500) String> evidenceUrls;
    }

    @Data
    public static class ResolveDisputeRequest {
        @NotNull
        private DisputeStatus resolution;

        @Size(max = 4000)
        private String adminNotes;

        private BigDecimal refundAmountGhs;
    }
}
