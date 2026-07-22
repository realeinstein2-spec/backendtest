package com.makershub.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${makershub.notifications.firebase.credentials:}")
    private String firebaseCredentials;

    @Value("${makershub.notifications.firebase.service-account-path:}")
    private String serviceAccountPath;

    @PostConstruct
    public void initialize() {
        try {
            InputStream inputStream = null;

            if (firebaseCredentials != null && !firebaseCredentials.isBlank()) {
                inputStream = new ByteArrayInputStream(firebaseCredentials.trim().getBytes(StandardCharsets.UTF_8));
                log.info("Loading Firebase credentials from inline JSON configuration.");
            } else if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
                File file = new File(serviceAccountPath.trim());
                if (file.exists() && file.isFile()) {
                    inputStream = new FileInputStream(file);
                    log.info("Loading Firebase credentials from service account file: {}", file.getAbsolutePath());
                } else {
                    log.warn("Firebase service account file specified at '{}' was not found.", serviceAccountPath);
                }
            }

            if (inputStream == null) {
                log.warn("Firebase credentials are not configured. Firebase Push will run in Mock mode.");
                return;
            }

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(inputStream))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase Application has been initialized successfully.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
        }
    }
}
