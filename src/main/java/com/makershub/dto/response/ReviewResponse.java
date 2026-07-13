package com.makershub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

public final class ReviewResponse {

    private ReviewResponse() {}

    @Data
    @Builder
    public static class ReviewDetailResponse {
        private UUID id;
        private UUID orderId;
        private UUID reviewerId;
        private UUID reviewedId;
        private Integer overallRating;
        private Integer qualityRating;
        private Integer timelinessRating;
        private Integer communicationRating;
        private String comment;
        private Instant createdAt;
    }
}
