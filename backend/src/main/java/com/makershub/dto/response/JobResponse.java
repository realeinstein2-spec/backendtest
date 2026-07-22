package com.makershub.dto.response;

import com.makershub.enums.JobStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class JobResponse {

    private JobResponse() {}

    @Data
    @Builder
    public static class JobDetailResponse {
        private UUID id;
        private UUID smeId;
        private String smeName;
        private String title;
        private String productType;
        private String sectorTag;
        private Integer quantity;
        private String specifications;
        private BigDecimal budgetMinGhs;
        private BigDecimal budgetMaxGhs;
        private LocalDate deadline;
        private List<String> attachmentUrls;
        private List<String> productImageUrls;
        private String deliveryAddress;
        private JobStatus status;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
