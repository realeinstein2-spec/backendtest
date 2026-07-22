package com.makershub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class OrderProgressResponse {
    private UUID id;
    private UUID orderId;
    private Integer percentage;
    private String stageName;
    private String notes;
    private String photoUrl;
    private Instant createdAt;
}
