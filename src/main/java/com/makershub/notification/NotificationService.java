package com.makershub.notification;

import com.makershub.enums.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final FirebasePushService firebasePushService;
    private final SmsService smsService;

    @Async
    public void sendNotification(NotificationEvent event) {
        try {
            firebasePushService.send(event);
        } catch (Exception ex) {
            log.warn("Firebase push failed for user {}, falling back to SMS: {}", event.getRecipientId(), ex.getMessage());
            smsService.send(event);
        }
    }

    @Async
    public void sendChannel(NotificationEvent event, NotificationChannel channel) {
        switch (channel) {
            case PUSH -> firebasePushService.send(event);
            case SMS -> smsService.send(event);
            default -> log.info("Notification channel {} not implemented", channel);
        }
    }
}
