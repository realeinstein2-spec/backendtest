package com.makershub.service;

import com.makershub.dto.request.MessageRequest;
import com.makershub.dto.response.MessageResponse;
import com.makershub.entity.Message;
import com.makershub.entity.Order;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.exception.UnauthorizedException;
import com.makershub.audit.AuditLogger;
import com.makershub.mapper.DtoMapper;
import com.makershub.notification.NotificationEvent;
import com.makershub.notification.NotificationService;
import com.makershub.repository.MessageRepository;
import com.makershub.repository.OrderRepository;
import com.makershub.repository.UserRepository;
import com.makershub.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DtoMapper mapper;
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;

    @Transactional
    public MessageResponse.MessageDetailResponse sendMessage(MessageRequest.CreateMessageRequest request) {
        User sender = getAuthenticatedUser();
        UUID orderId = UUID.fromString(request.getOrderId());
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.getOrderId()));
        if (!order.getSme().getId().equals(sender.getId()) && !order.getFactory().getId().equals(sender.getId())) {
            throw new UnauthorizedException("You cannot message on this order");
        }
        Message message = Message.builder()
                .order(order)
                .sender(sender)
                .content(request.getContent())
                .attachmentUrl(request.getAttachmentUrl())
                .isRead(false)
                .build();
        Message saved = messageRepository.save(message);
        auditLogger.log(AuditAction.CREATE, "MESSAGE", saved.getId(), null, null);
        User recipient = order.getSme().getId().equals(sender.getId()) ? order.getFactory() : order.getSme();
        notificationService.sendNotification(NotificationEvent.builder()
                .recipientId(recipient.getId())
                .phoneNumber(recipient.getPhoneNumber())
                .type(com.makershub.enums.NotificationType.MESSAGE_RECEIVED)
                .title("New message")
                .body("You have a new message on order " + orderId)
                .data(Map.of("orderId", orderId.toString(), "messageId", saved.getId().toString()))
                .build());
        return mapper.toMessageResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse.MessageDetailResponse> listMessages(UUID orderId, Pageable pageable) {
        return messageRepository.findByOrderIdOrderByCreatedAtAsc(orderId, pageable).map(mapper::toMessageResponse);
    }

    private User getAuthenticatedUser() {
        UserDetailsImpl principal = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId().toString()));
    }
}
