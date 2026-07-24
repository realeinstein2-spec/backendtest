package com.makershub.service;

import com.makershub.dto.request.OrderRequest;
import com.makershub.dto.response.OrderResponse;
import com.makershub.entity.*;
import com.makershub.enums.*;
import com.makershub.exception.BusinessException;
import com.makershub.audit.AuditLogger;
import com.makershub.mapper.DtoMapper;
import com.makershub.notification.NotificationService;
import com.makershub.repository.*;
import com.makershub.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private BidRepository bidRepository;
    @Mock
    private JobListingRepository jobRepository;
    @Mock
    private EscrowTransactionRepository escrowRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DtoMapper mapper;
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void updateStatus_rejectsInvalidTransition() {
        User factoryUser = user();
        authenticate(factoryUser);
        Order order = order(factoryUser, OrderStatus.PAYMENT_PENDING);
        order.setFactory(factoryUser); // Ensure logged-in user is the assigned factory
        when(orderRepository.findByIdAndDeletedAtIsNull(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findByIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factoryUser));

        OrderRequest.StatusUpdateRequest request = new OrderRequest.StatusUpdateRequest();
        request.setNewStatus(OrderStatus.DELIVERED);

        assertThatThrownBy(() -> orderService.updateStatus(order.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid order status transition");
    }

    @Test
    void confirmDelivery_rejectsExpiredQualityWindow() {
        User sme = user();
        authenticate(sme);
        Order order = order(sme, OrderStatus.DELIVERED);
        order.setQualityCheckDeadline(Instant.now().minus(java.time.Duration.ofHours(1)));
        when(orderRepository.findByIdAndDeletedAtIsNull(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findByIdAndDeletedAtIsNull(sme.getId())).thenReturn(Optional.of(sme));

        OrderRequest.ConfirmDeliveryRequest request = new OrderRequest.ConfirmDeliveryRequest();
        request.setQualityAccepted(true);

        assertThatThrownBy(() -> orderService.confirmDelivery(order.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quality check window has expired");
    }

    @Test
    void updateStatus_rejectsFactoryAttemptingToCancelOrder() {
        User factoryUser = user();
        authenticate(factoryUser);
        Order order = order(factoryUser, OrderStatus.PAYMENT_PENDING);
        order.setFactory(factoryUser);
        when(orderRepository.findByIdAndDeletedAtIsNull(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findByIdAndDeletedAtIsNull(factoryUser.getId())).thenReturn(Optional.of(factoryUser));

        OrderRequest.StatusUpdateRequest request = new OrderRequest.StatusUpdateRequest();
        request.setNewStatus(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> orderService.updateStatus(order.getId(), request))
                .isInstanceOf(com.makershub.exception.UnauthorizedException.class)
                .hasMessageContaining("Factories cannot cancel orders");
    }

    @Test
    void confirmDelivery_opensDisputeWhenQualityAcceptedIsNull() {
        User sme = user();
        authenticate(sme);
        Order order = order(sme, OrderStatus.DELIVERED);
        when(orderRepository.findByIdAndDeletedAtIsNull(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findByIdAndDeletedAtIsNull(sme.getId())).thenReturn(Optional.of(sme));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRequest.ConfirmDeliveryRequest request = new OrderRequest.ConfirmDeliveryRequest();
        request.setQualityAccepted(null);
        request.setComment("Quality issue");

        orderService.confirmDelivery(order.getId(), request);

        org.assertj.core.api.Assertions.assertThat(order.getStatus()).isEqualTo(OrderStatus.DISPUTED);
    }

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .phoneNumber("+233241234567")
                .fullName("Test User")
                .role(UserRole.FACTORY_OWNER)
                .isVerified(true)
                .isActive(true)
                .passwordHash("hash")
                .build();
    }

    private Order order(User party, OrderStatus status) {
        User other = user();
        other.setId(UUID.randomUUID());
        JobListing job = JobListing.builder().id(UUID.randomUUID()).build();
        Bid bid = Bid.builder().id(UUID.randomUUID()).build();
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .jobListing(job)
                .bid(bid)
                .sme(party)
                .factory(other)
                .agreedAmountGhs(BigDecimal.valueOf(1000))
                .platformFeeGhs(BigDecimal.valueOf(35))
                .factoryPayoutGhs(BigDecimal.valueOf(965))
                .status(status)
                .build();
        if (status == OrderStatus.DELIVERED) {
            order.setDeliveredAt(Instant.now());
            order.setQualityCheckDeadline(Instant.now().plus(java.time.Duration.ofHours(47)));
        }
        return order;
    }

    private void authenticate(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }
}
