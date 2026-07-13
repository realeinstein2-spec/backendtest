package com.makershub.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    @Value("${makershub.notifications.africastalking.enabled:false}")
    private boolean enabled;

    public void send(NotificationEvent event) {
        if (!enabled) {
            log.info("[SMS-SKIPPED] {} -> {}", event.getPhoneNumber(), event.getBody());
            return;
        }
        // Integration point: Africa's Talking SDK / REST call here.
        log.info("[SMS] Sent to {}: {}", event.getPhoneNumber(), event.getBody());
    }
}
