package com.makershub.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${makershub.notifications.firebase.credentials:}")
    private String firebaseCredentials;

    @PostConstruct
    public void initialize() {
        try {
            if (firebaseCredentials == null || firebaseCredentials.isBlank()) {
                log.warn("Firebase credentials are not configured. Firebase Push will run in Mock mode.");
                return;
            }
            if (FirebaseApp.getApps().isEmpty()) {
                ByteArrayInputStream stream = new ByteArrayInputStream(
                        firebaseCredentials.trim().getBytes(StandardCharsets.UTF_8)
                );
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase Application has been initialized successfully.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
        }
    }
}
