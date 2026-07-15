package com.makershub.service;

import com.makershub.dto.request.OrderRequest;
import com.makershub.dto.response.OrderResponse;
import com.makershub.entity.Bid;
import com.makershub.entity.EscrowTransaction;
import com.makershub.entity.Order;
import com.makershub.entity.User;
import com.makershub.enums.*;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.exception.UnauthorizedException;
import com.makershub.audit.AuditLogger;
import com.makershub.mapper.DtoMapper;
import com.makershub.notification.NotificationEvent;
import com.makershub.notification.NotificationService;
import com.makershub.repository.BidRepository;
import com.makershub.repository.EscrowTransactionRepository;
import com.makershub.repository.JobListingRepository;
import com.makershub.repository.OrderRepository;
import com.makershub.repository.UserRepository;
import com.makershub.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    @Value("${makershub.platform.fee-percent:0.035}")
    private BigDecimal platformFeePercent;

    private final OrderRepository orderRepository;
    private final BidRepository bidRepository;
    private final JobListingRepository jobRepository;
    private final EscrowTransactionRepository escrowRepository;
    private final UserRepository userRepository;
    private final DtoMapper mapper;
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;

    @Transactional
    public OrderResponse.OrderDetailResponse acceptBid(UUID bidId) {
        User sme = getAuthenticatedUser();
        if (sme.getRole() != UserRole.SME_OWNER && sme.getRole() != UserRole.ENTERPRISE) {
            throw new UnauthorizedException("Only SMEs can accept bids");
        }
        Bid bid = bidRepository.findByIdAndDeletedAtIsNull(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid", bidId.toString()));
        if (bid.getStatus() != BidStatus.PENDING) {
            throw new BusinessException("Bid is not pending", HttpStatus.CONFLICT, "BID_NOT_PENDING");
        }
        if (!bid.getJobListing().getSme().getId().equals(sme.getId())) {
            throw new UnauthorizedException("You can only accept bids on your own jobs");
        }
        bid.setStatus(BidStatus.ACCEPTED);
        bidRepository.save(bid);

        // H-4: Reject all other pending bids for the same job
        List<Bid> otherBids = bidRepository.findByJobListingIdAndDeletedAtIsNullOrderByTotalPriceGhsAsc(bid.getJobListing().getId());
        for (Bid other : otherBids) {
            if (!other.getId().equals(bid.getId()) && other.getStatus() == BidStatus.PENDING) {
                other.setStatus(BidStatus.REJECTED);
                bidRepository.save(other);
            }
        }

        BigDecimal agreed = bid.getTotalPriceGhs();
        BigDecimal fee = agreed.multiply(platformFeePercent).setScale(2, RoundingMode.HALF_UP);
        Order order = Order.builder()
                .jobListing(bid.getJobListing())
                .bid(bid)
                .sme(sme)
                .factory(bid.getFactory().getUser())
                .agreedAmountGhs(agreed)
                .platformFeeGhs(fee)
                .factoryPayoutGhs(agreed.subtract(fee))
                .status(OrderStatus.PAYMENT_PENDING)
                .build();
        Order saved = orderRepository.save(order);
        bid.getJobListing().setStatus(JobStatus.AWARDED);
        jobRepository.save(bid.getJobListing());
        auditLogger.log(AuditAction.CREATE, "ORDER", saved.getId(), null, null);
        notify(saved, NotificationType.BID_ACCEPTED, "Bid accepted", "Your bid was accepted. Proceed to payment.");
        return mapper.toOrderResponse(saved);
    }

    @Transactional
    public OrderResponse.OrderDetailResponse updateStatus(UUID orderId, OrderRequest.StatusUpdateRequest request) {
        User factoryUser = getAuthenticatedUser();
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        if (!order.getFactory().getId().equals(factoryUser.getId())) {
            throw new UnauthorizedException("Only the assigned factory can update this order");
        }
        validateTransition(order.getStatus(), request.getNewStatus());
        // M-1: Capture old status before mutation so audit log is accurate
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(request.getNewStatus());
        if (request.getNewStatus() == OrderStatus.IN_PRODUCTION) {
            order.getJobListing().setStatus(JobStatus.IN_PRODUCTION);
            jobRepository.save(order.getJobListing());
        }
        if (request.getNewStatus() == OrderStatus.DELIVERED) {
            order.setDeliveredAt(java.time.Instant.now());
            order.setQualityCheckDeadline(java.time.Instant.now().plus(java.time.Duration.ofHours(48)));
        }
        Order saved = orderRepository.save(order);
        auditLogger.log(AuditAction.UPDATE, "ORDER", saved.getId(), oldStatus.name(), saved.getStatus().name());
        notify(saved, NotificationType.ORDER_STATUS_UPDATE, "Order update", "Order status changed to " + saved.getStatus());
        return mapper.toOrderResponse(saved);
    }

    @Transactional
    public OrderResponse.OrderDetailResponse confirmDelivery(UUID orderId, OrderRequest.ConfirmDeliveryRequest request) {
        User sme = getAuthenticatedUser();
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        if (!order.getSme().getId().equals(sme.getId())) {
            throw new UnauthorizedException("Only the SME can confirm delivery");
        }
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.QUALITY_CHECK) {
            throw new BusinessException("Order is not in delivery/quality check state", HttpStatus.CONFLICT, "ORDER_NOT_DELIVERED");
        }
        if (order.getQualityCheckDeadline() != null && order.getQualityCheckDeadline().isBefore(java.time.Instant.now())) {
            throw new BusinessException("Quality check window has expired", HttpStatus.CONFLICT, "QUALITY_WINDOW_EXPIRED");
        }
        if (Boolean.FALSE.equals(request.getQualityAccepted())) {
            return openDispute(order, sme, request.getComment());
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(java.time.Instant.now());
        Order saved = orderRepository.save(order);
        releaseEscrow(saved);
        auditLogger.log(AuditAction.UPDATE, "ORDER", saved.getId(), OrderStatus.DELIVERED.name(), OrderStatus.COMPLETED.name());
        notify(saved, NotificationType.ORDER_STATUS_UPDATE, "Order completed", "Quality confirmed. Escrow released.");
        return mapper.toOrderResponse(saved);
    }

    // H-7: Allow SME to cancel an order that is still awaiting payment
    @Transactional
    public OrderResponse.OrderDetailResponse cancelOrder(UUID orderId) {
        User user = getAuthenticatedUser();
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        if (!order.getSme().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the SME can cancel this order");
        }
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException("Order can only be cancelled when awaiting payment",
                    HttpStatus.CONFLICT, "ORDER_CANNOT_BE_CANCELLED");
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        auditLogger.log(AuditAction.UPDATE, "ORDER", saved.getId(), OrderStatus.PAYMENT_PENDING.name(), OrderStatus.CANCELLED.name());
        notify(saved, NotificationType.ORDER_STATUS_UPDATE, "Order cancelled", "Order has been cancelled by the SME.");
        return mapper.toOrderResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse.OrderDetailResponse getOrder(UUID orderId) {
        User user = getAuthenticatedUser();
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        if (!order.getSme().getId().equals(user.getId()) && !order.getFactory().getId().equals(user.getId())) {
            throw new UnauthorizedException("Access denied for this order");
        }
        return mapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse.OrderDetailResponse> listMyOrders(Pageable pageable) {
        User user = getAuthenticatedUser();
        if (user.getRole() == UserRole.FACTORY_OWNER) {
            return orderRepository.findByFactoryIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId(), pageable)
                    .map(mapper::toOrderResponse);
        }
        return orderRepository.findBySmeIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId(), pageable)
                .map(mapper::toOrderResponse);
    }

    private OrderResponse.OrderDetailResponse openDispute(Order order, User sme, String comment) {
        order.setStatus(OrderStatus.DISPUTED);
        Order saved = orderRepository.save(order);
        auditLogger.log(AuditAction.DISPUTE_RESOLVE, "ORDER", saved.getId(), OrderStatus.DELIVERED.name(), OrderStatus.DISPUTED.name());
        notify(saved, NotificationType.DISPUTE_OPENED, "Dispute opened", "A dispute has been opened: " + comment);
        return mapper.toOrderResponse(saved);
    }

    private void releaseEscrow(Order order) {
        escrowRepository.findByOrderId(order.getId()).ifPresent(escrow -> {
            escrow.setEscrowStatus(EscrowStatus.RELEASED);
            escrow.setReleasedAt(java.time.Instant.now());
            escrowRepository.save(escrow);
            auditLogger.log(AuditAction.ESCROW_RELEASE, "ESCROW", escrow.getId(), EscrowStatus.HELD.name(), EscrowStatus.RELEASED.name());
        });
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        Set<OrderStatus> allowed = switch (current) {
            // C-4: IN_ESCROW removed — only PaymentService webhook can set IN_ESCROW
            case PAYMENT_PENDING -> Set.of(OrderStatus.CANCELLED);
            case IN_ESCROW -> Set.of(OrderStatus.IN_PRODUCTION, OrderStatus.REFUNDED);
            case IN_PRODUCTION -> Set.of(OrderStatus.QUALITY_CHECK);
            case QUALITY_CHECK -> Set.of(OrderStatus.DELIVERED);
            case DELIVERED -> Set.of(OrderStatus.COMPLETED, OrderStatus.DISPUTED);
            default -> Set.of();
        };
        if (!allowed.contains(next)) {
            throw new BusinessException("Invalid order status transition from " + current + " to " + next,
                    HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION");
        }
    }

    private void notify(Order order, NotificationType type, String title, String body) {
        notificationService.sendNotification(NotificationEvent.builder()
                .recipientId(order.getSme().getId())
                .phoneNumber(order.getSme().getPhoneNumber())
                .type(type)
                .title(title)
                .body(body)
                .data(Map.of("orderId", order.getId().toString()))
                .build());
        notificationService.sendNotification(NotificationEvent.builder()
                .recipientId(order.getFactory().getId())
                .phoneNumber(order.getFactory().getPhoneNumber())
                .type(type)
                .title(title)
                .body(body)
                .data(Map.of("orderId", order.getId().toString()))
                .build());
    }

    // M-20: Each order is auto-completed independently; errors are caught per-order to avoid blocking the batch
    @Transactional
    public void autoCompleteExpiredOrders() {
        java.time.Instant now = java.time.Instant.now();
        java.util.List<Order> expired = orderRepository.findByStatusAndQualityCheckDeadlineBeforeAndDeletedAtIsNull(
                OrderStatus.DELIVERED, now);
        for (Order order : expired) {
            try {
                order.setStatus(OrderStatus.COMPLETED);
                order.setCompletedAt(now);
                Order saved = orderRepository.save(order);
                releaseEscrow(saved);
                auditLogger.log(AuditAction.UPDATE, "ORDER", saved.getId(), OrderStatus.DELIVERED.name(), OrderStatus.COMPLETED.name());
                notify(saved, NotificationType.ORDER_STATUS_UPDATE, "Order completed automatically",
                        "Quality check window expired. Escrow released.");
            } catch (Exception e) {
                log.error("Failed to auto-complete order {}: {}", order.getId(), e.getMessage(), e);
            }
        }
    }

    private User getAuthenticatedUser() {
        UserDetailsImpl principal = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId().toString()));
    }
}
