package com.makershub.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class JobRequest {

    private JobRequest() {}

    @Data
    public static class CreateJobRequest {
        @NotBlank
        @Size(max = 200)
        private String title;

        @NotBlank
        @Size(max = 100)
        private String productType;

        @NotBlank
        @Size(max = 50)
        private String sectorTag;

        @NotNull
        @Min(1)
        private Integer quantity;

        @Size(max = 4000)
        private String specifications;

        @DecimalMin(value = "0.00", inclusive = false)
        private BigDecimal budgetMinGhs;

        @DecimalMin(value = "0.00", inclusive = false)
        private BigDecimal budgetMaxGhs;

        @NotNull
        @Future
        private LocalDate deadline;

        @Size(max = 500)
        private String deliveryAddress;

        private List<@Size(max = 500) String> attachmentUrls;
    }
}
