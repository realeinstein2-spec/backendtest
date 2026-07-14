package com.makershub.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    public void send(NotificationEvent event) {
        // Log-only SMS channel for development/testing
        log.info("==========================================================");
        log.info("[SMS LOG CHANNEL] To: {}, Message: {}", event.getPhoneNumber(), event.getBody());
        log.info("==========================================================");
    }

    /*
     * TO ENABLE AFRICA'S TALKING SMS IN PRODUCTION:
     * 1. Uncomment the dependency injection and fields below.
     * 2. Replace the send() method with the commented REST API code.
     *
    
    private final org.springframework.web.client.RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${makershub.notifications.africastalking.enabled:false}")
    private boolean enabled;

    @org.springframework.beans.factory.annotation.Value("${makershub.notifications.africastalking.username:}")
    private String username;

    @org.springframework.beans.factory.annotation.Value("${makershub.notifications.africastalking.api-key:}")
    private String apiKey;

    public void send(NotificationEvent event) {
        if (!enabled) {
            log.info("[SMS-SKIPPED] {} -> {}", event.getPhoneNumber(), event.getBody());
            return;
        }

        if (username.isBlank() || apiKey.isBlank()) {
            log.warn("Africa's Talking SMS is enabled but username or API Key is not configured.");
            return;
        }

        String url = "sandbox".equalsIgnoreCase(username.trim())
                ? "https://api.sandbox.africastalking.com/version1/messaging"
                : "https://api.africastalking.com/version1/messaging";

        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(java.util.List.of(org.springframework.http.MediaType.APPLICATION_JSON));
            headers.set("apiKey", apiKey.trim());

            org.springframework.util.MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("username", username.trim());
            body.add("to", event.getPhoneNumber());
            body.add("message", event.getBody());

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> requestEntity = 
                    new org.springframework.http.HttpEntity<>(body, headers);

            log.info("Sending SMS to {} via Africa's Talking...", event.getPhoneNumber());
            org.springframework.http.ResponseEntity<String> response = 
                    restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[SMS-SUCCESS] SMS sent successfully. Response: {}", response.getBody());
            } else {
                log.error("[SMS-FAILED] Failed to send SMS. Status: {}, Response: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception ex) {
            log.error("[SMS-ERROR] Exception occurred while sending SMS to {}: {}", event.getPhoneNumber(), ex.getMessage(), ex);
        }
    }
    */
}
