package com.makershub.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class BidRequest {

    private BidRequest() {}

    @Data
    public static class CreateBidRequest {
        @NotBlank
        private String jobId;

        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal pricePerUnitGhs;

        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal totalPriceGhs;

        @NotNull
        @Min(1)
        private Integer productionDays;

        @NotNull
        @Future
        private LocalDate deliveryDateEstimate;

        @Size(max = 2000)
        private String message;
    }
}
