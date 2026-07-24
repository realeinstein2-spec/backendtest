package com.makershub.service;

import com.makershub.entity.Order;
import com.makershub.enums.OrderStatus;
import com.makershub.audit.AuditLogger;
import com.makershub.enums.AuditAction;
import com.makershub.enums.NotificationType;
import com.makershub.notification.NotificationEvent;
import com.makershub.notification.NotificationService;
import com.makershub.repository.EscrowTransactionRepository;
import com.makershub.repository.OrderRepository;
import com.makershub.entity.EscrowTransaction;
import com.makershub.enums.EscrowStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Helper component to allow per-order auto-completion in its own independent transaction.
 * This prevents a single failing order from rolling back the entire batch (BUG-01 fix).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCompleteHelper {

    private final OrderRepository orderRepository;
    private final EscrowTransactionRepository escrowRepository;
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;

    /**
     * Completes a single delivered order in its own REQUIRES_NEW transaction.
     * If this throws, only this order is rolled back — other orders in the batch are unaffected.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void autoCompleteSingleOrder(Order order) {
        Instant now = Instant.now();
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(now);
        Order saved = orderRepository.save(order);

        // Release escrow
        escrowRepository.findByOrderId(saved.getId()).ifPresent(escrow -> {
            escrow.setEscrowStatus(EscrowStatus.RELEASED);
            escrow.setReleasedAt(now);
            escrowRepository.save(escrow);
            auditLogger.log(AuditAction.ESCROW_RELEASE, "ESCROW", escrow.getId(),
                    EscrowStatus.HELD.name(), EscrowStatus.RELEASED.name());
        });

        auditLogger.log(AuditAction.UPDATE, "ORDER", saved.getId(),
                OrderStatus.DELIVERED.name(), OrderStatus.COMPLETED.name());

        notificationService.sendNotification(NotificationEvent.builder()
                .recipientId(saved.getSme().getId())
                .phoneNumber(saved.getSme().getPhoneNumber())
                .type(NotificationType.ORDER_STATUS_UPDATE)
                .title("Order completed automatically")
                .body("Quality check window expired. Escrow released.")
                .data(Map.of("orderId", saved.getId().toString()))
                .build());
        notificationService.sendNotification(NotificationEvent.builder()
                .recipientId(saved.getFactory().getId())
                .phoneNumber(saved.getFactory().getPhoneNumber())
                .type(NotificationType.ORDER_STATUS_UPDATE)
                .title("Order completed automatically")
                .body("Quality check window expired. Escrow released.")
                .data(Map.of("orderId", saved.getId().toString()))
                .build());
    }
}
