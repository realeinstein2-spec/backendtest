package com.makershub.service;

import com.makershub.dto.request.DisputeRequest;
import com.makershub.dto.response.DisputeResponse;
import com.makershub.entity.Dispute;
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
import com.makershub.repository.DisputeRepository;
import com.makershub.repository.OrderRepository;
import com.makershub.repository.UserRepository;
import com.makershub.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DtoMapper mapper;
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;

    @Transactional
    public DisputeResponse.DisputeDetailResponse raiseDispute(UUID orderId, DisputeRequest.CreateDisputeRequest request) {
        User user = getAuthenticatedUser();
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        if (!order.getSme().getId().equals(user.getId()) && !order.getFactory().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only order parties can raise a dispute");
        }
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.QUALITY_CHECK && order.getStatus() != OrderStatus.DISPUTED) {
            throw new BusinessException("Dispute can only be raised during quality check or after delivery", HttpStatus.CONFLICT, "INVALID_DISPUTE_STATE");
        }
        if (order.getQualityCheckDeadline() != null && order.getQualityCheckDeadline().isBefore(java.time.Instant.now())) {
            throw new BusinessException("Dispute window has expired", HttpStatus.CONFLICT, "DISPUTE_WINDOW_EXPIRED");
        }
        if (disputeRepository.findByOrderId(orderId).isPresent()) {
            throw new BusinessException("Dispute already exists for this order", HttpStatus.CONFLICT, "DISPUTE_EXISTS");
        }
        order.setStatus(OrderStatus.DISPUTED);
        orderRepository.save(order);
        Dispute dispute = Dispute.builder()
                .order(order)
                .raisedBy(user)
                .reason(request.getReason())
                .description(request.getDescription())
                .evidenceUrls(request.getEvidenceUrls())
                .status(DisputeStatus.OPEN)
                .build();
        Dispute saved = disputeRepository.save(dispute);
        auditLogger.log(AuditAction.CREATE, "DISPUTE", saved.getId(), null, null);
        notify(saved, NotificationType.DISPUTE_OPENED, "Dispute opened", "A dispute has been opened on order " + orderId);
        return mapper.toDisputeResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<DisputeResponse.DisputeDetailResponse> listDisputes(Pageable pageable) {
        return disputeRepository.findByStatusInOrderByCreatedAtAsc(List.of(DisputeStatus.OPEN, DisputeStatus.UNDER_REVIEW), pageable)
                .map(mapper::toDisputeResponse);
    }

    @Transactional
    public DisputeResponse.DisputeDetailResponse resolveDispute(UUID disputeId, DisputeRequest.ResolveDisputeRequest request) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", disputeId.toString()));
        dispute.setStatus(request.getResolution());
        dispute.setAdminNotes(request.getAdminNotes());
        dispute.setResolutionAmountGhs(request.getRefundAmountGhs());
        dispute.setResolvedAt(Instant.now());
        Dispute saved = disputeRepository.save(dispute);
        Order order = dispute.getOrder();
        order.setStatus(request.getResolution() == DisputeStatus.RESOLVED_BUYER ? OrderStatus.REFUNDED : OrderStatus.COMPLETED);
        orderRepository.save(order);
        auditLogger.log(AuditAction.DISPUTE_RESOLVE, "DISPUTE", saved.getId(), DisputeStatus.OPEN.name(), saved.getStatus().name());
        notify(saved, NotificationType.DISPUTE_RESOLVED, "Dispute resolved", "Your dispute has been resolved with status " + saved.getStatus());
        return mapper.toDisputeResponse(saved);
    }

    private void notify(Dispute dispute, NotificationType type, String title, String body) {
        notificationService.sendNotification(NotificationEvent.builder()
                .recipientId(dispute.getOrder().getSme().getId())
                .phoneNumber(dispute.getOrder().getSme().getPhoneNumber())
                .type(type).title(title).body(body)
                .data(Map.of("orderId", dispute.getOrder().getId().toString(), "disputeId", dispute.getId().toString()))
                .build());
        notificationService.sendNotification(NotificationEvent.builder()
                .recipientId(dispute.getOrder().getFactory().getId())
                .phoneNumber(dispute.getOrder().getFactory().getPhoneNumber())
                .type(type).title(title).body(body)
                .data(Map.of("orderId", dispute.getOrder().getId().toString(), "disputeId", dispute.getId().toString()))
                .build());
    }

    private User getAuthenticatedUser() {
        UserDetailsImpl principal = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId().toString()));
    }
}
