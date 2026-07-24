package com.makershub.service;

import com.makershub.audit.AuditLogger;
import com.makershub.entity.EscrowTransaction;
import com.makershub.entity.Order;
import com.makershub.entity.User;
import com.makershub.enums.EscrowStatus;
import com.makershub.enums.OrderStatus;
import com.makershub.enums.UserRole;
import com.makershub.notification.NotificationService;
import com.makershub.repository.EscrowTransactionRepository;
import com.makershub.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderAutoCompleteHelperTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EscrowTransactionRepository escrowRepository;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderAutoCompleteHelper orderAutoCompleteHelper;

    private User sme;
    private User factory;
    private Order deliveredOrder;
    private EscrowTransaction escrowTransaction;

    @BeforeEach
    void setUp() {
        sme = User.builder().id(UUID.randomUUID()).phoneNumber("+233240000001").role(UserRole.SME_OWNER).build();
        factory = User.builder().id(UUID.randomUUID()).phoneNumber("+233240000002").role(UserRole.FACTORY_OWNER).build();

        deliveredOrder = Order.builder()
                .id(UUID.randomUUID())
                .sme(sme)
                .factory(factory)
                .status(OrderStatus.DELIVERED)
                .build();

        escrowTransaction = EscrowTransaction.builder()
                .id(UUID.randomUUID())
                .order(deliveredOrder)
                .escrowStatus(EscrowStatus.HELD)
                .build();
    }

    @Test
    void autoCompleteSingleOrder_updatesStatusAndReleasesEscrow() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowRepository.findByOrderId(deliveredOrder.getId())).thenReturn(Optional.of(escrowTransaction));

        orderAutoCompleteHelper.autoCompleteSingleOrder(deliveredOrder);

        assertThat(deliveredOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(deliveredOrder.getCompletedAt()).isNotNull();
        assertThat(escrowTransaction.getEscrowStatus()).isEqualTo(EscrowStatus.RELEASED);

        verify(orderRepository).save(deliveredOrder);
        verify(escrowRepository).save(escrowTransaction);
        verify(auditLogger, times(2)).log(any(), any(), any(), any(), any());
        verify(notificationService, times(2)).sendNotification(any());
    }
}
