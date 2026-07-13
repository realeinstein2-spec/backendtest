package com.makershub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

public final class MessageResponse {

    private MessageResponse() {}

    @Data
    @Builder
    public static class MessageDetailResponse {
        private UUID id;
        private UUID orderId;
        private UUID senderId;
        private String senderName;
        private String content;
        private String attachmentUrl;
        private Boolean isRead;
        private Instant createdAt;
    }
}
