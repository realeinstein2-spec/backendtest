package com.makershub.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

public final class ReviewRequest {

    private ReviewRequest() {}

    @Data
    public static class CreateReviewRequest {
        @NotBlank
        private String orderId;

        @NotNull
        @Min(1)
        @Max(5)
        private Integer overallRating;

        @Min(1)
        @Max(5)
        private Integer qualityRating;

        @Min(1)
        @Max(5)
        private Integer timelinessRating;

        @Min(1)
        @Max(5)
        private Integer communicationRating;

        @Size(max = 2000)
        private String comment;
    }
}
