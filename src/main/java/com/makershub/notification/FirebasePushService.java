package com.makershub.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FirebasePushService {

    @Value("${makershub.notifications.firebase.enabled:false}")
    private boolean enabled;

    public void send(NotificationEvent event) {
        if (!enabled) {
            log.info("[FB-PUSH-SKIPPED] {}", event);
            return;
        }
        // Integration point: Firebase Admin SDK would send the push here.
        log.info("[FB-PUSH] Sent to {}: {}", event.getRecipientId(), event.getTitle());
    }
}
