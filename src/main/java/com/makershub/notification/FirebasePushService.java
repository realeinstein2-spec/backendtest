package com.makershub.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.makershub.entity.User;
import com.makershub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebasePushService {

    private final UserRepository userRepository;

    @Value("${makershub.notifications.firebase.enabled:false}")
    private boolean enabled;

    public void send(NotificationEvent event) {
        if (!enabled) {
            log.info("[FB-PUSH-MOCK] {}", event);
            return;
        }

        User recipient = userRepository.findById(event.getRecipientId()).orElse(null);
        if (recipient == null || recipient.getFcmToken() == null || recipient.getFcmToken().isBlank()) {
            log.warn("Cannot send Firebase Push. User {} has no registered FCM token.", event.getRecipientId());
            throw new IllegalStateException("Recipient has no registered device token");
        }

        try {
            Message message = Message.builder()
                    .setToken(recipient.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(event.getTitle())
                            .setBody(event.getBody())
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[FB-PUSH-SUCCESS] Sent message ID: {}", response);
        } catch (Exception e) {
            log.error("[FB-PUSH-FAILED] Failed to send push notification to user {}", event.getRecipientId(), e);
            throw new RuntimeException("FCM transmission failed: " + e.getMessage(), e);
        }
    }
}
