package com.makershub.service;

import com.makershub.entity.Factory;
import com.makershub.entity.JobListing;
import com.makershub.enums.NotificationType;
import com.makershub.notification.NotificationEvent;
import com.makershub.notification.NotificationService;
import com.makershub.repository.FactoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobNotificationService {

    private final FactoryRepository factoryRepository;
    private final NotificationService notificationService;

    @Async
    public void notifyMatchingFactories(JobListing job) {
        try {
            List<Factory> factories = factoryRepository.findMatchingFactories(
                    job.getSectorTag(), job.getQuantity(), 6.6885, -1.6244, 50000.0);
            
            factories.forEach(factory ->
                notificationService.sendNotification(NotificationEvent.builder()
                        .recipientId(factory.getUser().getId())
                        .phoneNumber(factory.getUser().getPhoneNumber())
                        .type(NotificationType.ORDER_STATUS_UPDATE)
                        .title("New job match")
                        .body("A new " + job.getSectorTag() + " job is available for bidding.")
                        .data(Map.of("jobId", job.getId().toString()))
                        .build())
            );
        } catch (Exception ex) {
            log.error("Failed async notification for matching factories on job {}: {}", job.getId(), ex.getMessage(), ex);
        }
    }
}
