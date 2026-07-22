package com.makershub.notification;

import com.makershub.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class NotificationEvent {
    private UUID recipientId;
    private String phoneNumber;
    private NotificationType type;
    private String title;
    private String body;
    private Map<String, String> data;
}
