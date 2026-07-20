package com.makershub.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final RestTemplate restTemplate;

    @Value("${makershub.notifications.africastalking.enabled:false}")
    private boolean enabled;

    @Value("${makershub.notifications.africastalking.username:}")
    private String username;

    @Value("${makershub.notifications.africastalking.api-key:}")
    private String apiKey;

    public void send(NotificationEvent event) {
        if (!enabled) {
            log.info("==========================================================");
            log.info("[SMS LOG CHANNEL (MOCK)] To: {}, Message: {}", event.getPhoneNumber(), event.getBody());
            log.info("==========================================================");
            return;
        }

        if (username == null || username.isBlank() || apiKey == null || apiKey.isBlank()) {
            log.warn("Africa's Talking SMS is enabled but username or API Key is not configured. Falling back to Log.");
            log.info("[SMS LOG FALLBACK] To: {}, Message: {}", event.getPhoneNumber(), event.getBody());
            return;
        }

        String url = "sandbox".equalsIgnoreCase(username.trim())
                ? "https://api.sandbox.africastalking.com/version1/messaging"
                : "https://api.africastalking.com/version1/messaging";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("apiKey", apiKey.trim());

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("username", username.trim());
            body.add("to", event.getPhoneNumber());
            body.add("message", event.getBody());

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            log.info("Sending SMS to {} via Africa's Talking...", event.getPhoneNumber());
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[SMS-SUCCESS] SMS sent successfully. Response: {}", response.getBody());
            } else {
                log.error("[SMS-FAILED] Failed to send SMS. Status: {}, Response: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception ex) {
            log.error("[SMS-ERROR] Exception occurred while sending SMS to {}: {}", event.getPhoneNumber(), ex.getMessage(), ex);
        }
    }
}
