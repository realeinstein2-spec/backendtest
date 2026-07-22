package com.makershub.notification.service;

import com.makershub.notification.dto.NotificationResponse;
import com.makershub.notification.entity.Notification;
import com.makershub.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationAppServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationAppService notificationAppService;

    @Test
    void getNotifications_returnsUserNotifications() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .recipientId(userId)
                .title("Order Confirmed")
                .body("Payment received")
                .isRead(false)
                .build();

        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(notification)));

        Page<NotificationResponse.NotificationDetailResponse> result =
                notificationAppService.getNotifications(userId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Order Confirmed");
    }

    @Test
    void markAsRead_success() {
        UUID userId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .id(notifId)
                .recipientId(userId)
                .isRead(false)
                .build();

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));

        notificationAppService.markAsRead(userId, notifId);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_throwsForbidden_whenWrongUser() {
        UUID userId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .id(notifId)
                .recipientId(otherUser)
                .build();

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationAppService.markAsRead(userId, notifId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Access denied");
    }
}
