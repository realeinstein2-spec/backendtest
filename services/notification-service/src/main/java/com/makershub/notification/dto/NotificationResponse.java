package com.makershub.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

public final class NotificationResponse {
    private NotificationResponse() {}

    @Data
    @Builder
    public static class NotificationDetailResponse {
        private UUID id;
        private UUID recipientId;
        private String title;
        private String body;
        private boolean isRead;
        private Instant createdAt;
    }
}
