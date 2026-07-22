package com.makershub.marketplace.dto;

import com.makershub.marketplace.enums.JobStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class JobDto {

    private JobDto() {}

    @Data
    public static class CreateJobRequest {
        private String title;
        private String description;
        private String category;
        private Integer quantity;
        private BigDecimal targetPrice;
        private Instant deadline;
    }

    @Data
    @Builder
    public static class JobDetailResponse {
        private UUID id;
        private UUID smeOwnerId;
        private String title;
        private String description;
        private String category;
        private Integer quantity;
        private BigDecimal targetPrice;
        private String currency;
        private Instant deadline;
        private JobStatus status;
        private Instant createdAt;
    }
}
