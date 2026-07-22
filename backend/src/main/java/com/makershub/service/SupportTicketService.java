package com.makershub.service;

import com.makershub.audit.AuditLogger;
import com.makershub.dto.request.SupportTicketRequest;
import com.makershub.dto.response.SupportTicketResponse;
import com.makershub.entity.SupportMessage;
import com.makershub.entity.SupportTicket;
import com.makershub.entity.User;
import com.makershub.enums.*;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.exception.UnauthorizedException;
import com.makershub.notification.NotificationEvent;
import com.makershub.notification.NotificationService;
import com.makershub.repository.SupportMessageRepository;
import com.makershub.repository.SupportTicketRepository;
import com.makershub.repository.UserRepository;
import com.makershub.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    public static final int SLA_WINDOW_HOURS = 12;

    private final SupportTicketRepository supportTicketRepository;
    private final SupportMessageRepository supportMessageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;

    /**
     * Create a new support ticket requested by the authenticated user.
     * Sets target SLA deadline to 12 hours from creation time.
     */
    @Transactional
    public SupportTicketResponse.SupportTicketDetailResponse createTicket(SupportTicketRequest.CreateTicketRequest request) {
        User currentUser = getCurrentUser();
        Instant now = Instant.now();
        Instant slaDeadline = now.plus(Duration.ofHours(SLA_WINDOW_HOURS));
        String ticketNumber = generateTicketNumber();

        SupportTicket ticket = SupportTicket.builder()
                .ticketNumber(ticketNumber)
                .user(currentUser)
                .subject(request.getSubject())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(request.getPriority() != null ? request.getPriority() : SupportTicketPriority.MEDIUM)
                .status(SupportTicketStatus.OPEN)
                .targetSlaDeadline(slaDeadline)
                .attachmentUrl(request.getAttachmentUrl())
                .build();

        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        // Add initial message to thread
        SupportMessage initialMessage = SupportMessage.builder()
                .ticket(savedTicket)
                .sender(currentUser)
                .message(request.getDescription())
                .attachmentUrl(request.getAttachmentUrl())
                .build();
        supportMessageRepository.save(initialMessage);

        auditLogger.log(AuditAction.CREATE, "SUPPORT_TICKET", savedTicket.getId(),
                "Support ticket created with 12h SLA", currentUser.getId());

        notificationService.sendNotification(NotificationEvent.builder()
                .recipientId(currentUser.getId())
                .phoneNumber(currentUser.getPhoneNumber())
                .type(NotificationType.SUPPORT_TICKET_CREATED)
                .title("Support Request Received - #" + ticketNumber)
                .body("Your support request has been logged. An admin will attend to it within 12 hours.")
                .data(Map.of("ticketId", savedTicket.getId().toString(), "ticketNumber", ticketNumber))
                .build());

        return toDetailResponse(savedTicket);
    }

    /**
     * Get paginated support tickets created by the authenticated user.
     */
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse.SupportTicketSummaryResponse> getUserTickets(Pageable pageable) {
        User currentUser = getCurrentUser();
        return supportTicketRepository.findByUserIdAndDeletedAtIsNull(currentUser.getId(), pageable)
                .map(this::toSummaryResponse);
    }

    /**
     * Get details for a specific support ticket.
     * Enforces ownership unless requester is an Admin.
     */
    @Transactional(readOnly = true)
    public SupportTicketResponse.SupportTicketDetailResponse getTicketDetails(UUID ticketId) {
        User currentUser = getCurrentUser();
        SupportTicket ticket = supportTicketRepository.findByIdAndDeletedAtIsNull(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId.toString()));

        if (!ticket.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You do not have permission to view this support ticket");
        }

        return toDetailResponse(ticket);
    }

    /**
     * Add a reply message to a support ticket thread.
     */
    @Transactional
    public SupportTicketResponse.SupportMessageDetailResponse addMessage(UUID ticketId, SupportTicketRequest.AddMessageRequest request) {
        User currentUser = getCurrentUser();
        SupportTicket ticket = supportTicketRepository.findByIdAndDeletedAtIsNull(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId.toString()));

        if (!ticket.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You do not have permission to reply to this support ticket");
        }

        SupportMessage message = SupportMessage.builder()
                .ticket(ticket)
                .sender(currentUser)
                .message(request.getMessage())
                .attachmentUrl(request.getAttachmentUrl())
                .build();

        SupportMessage savedMessage = supportMessageRepository.save(message);

        // Update ticket status based on who replied
        if (currentUser.getRole() == UserRole.ADMIN) {
            if (ticket.getStatus() == SupportTicketStatus.OPEN) {
                ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
            }
            if (ticket.getAssignedAdmin() == null) {
                ticket.setAssignedAdmin(currentUser);
            }
            // Notify user of admin reply
            notificationService.sendNotification(NotificationEvent.builder()
                    .recipientId(ticket.getUser().getId())
                    .phoneNumber(ticket.getUser().getPhoneNumber())
                    .type(NotificationType.SUPPORT_TICKET_UPDATED)
                    .title("New Reply on Support Ticket #" + ticket.getTicketNumber())
                    .body("An admin replied to your support ticket.")
                    .data(Map.of("ticketId", ticket.getId().toString(), "ticketNumber", ticket.getTicketNumber()))
                    .build());
        } else {
            if (ticket.getStatus() == SupportTicketStatus.WAITING_FOR_USER) {
                ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
            }
        }
        supportTicketRepository.save(ticket);

        return toMessageResponse(savedMessage);
    }

    /**
     * Admin endpoint: Retrieve all support tickets with optional filtering.
     */
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse.SupportTicketSummaryResponse> adminGetAllTickets(SupportTicketStatus status, SupportTicketCategory category, Pageable pageable) {
        if (status != null) {
            return supportTicketRepository.findByStatusAndDeletedAtIsNull(status, pageable).map(this::toSummaryResponse);
        }
        if (category != null) {
            return supportTicketRepository.findByCategoryAndDeletedAtIsNull(category, pageable).map(this::toSummaryResponse);
        }
        return supportTicketRepository.findAllByDeletedAtIsNull(pageable).map(this::toSummaryResponse);
    }

    /**
     * Admin endpoint: Update support ticket status, assign admin, and resolution notes.
     */
    @Transactional
    public SupportTicketResponse.SupportTicketDetailResponse adminUpdateTicketStatus(UUID ticketId, SupportTicketRequest.UpdateTicketStatusRequest request) {
        User admin = getCurrentUser();
        SupportTicket ticket = supportTicketRepository.findByIdAndDeletedAtIsNull(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId.toString()));

        ticket.setStatus(request.getStatus());
        if (request.getAdminNotes() != null) {
            ticket.setAdminNotes(request.getAdminNotes());
        }

        if (request.getAssignedAdminId() != null && !request.getAssignedAdminId().isBlank()) {
            User assignedAdmin = userRepository.findByIdAndDeletedAtIsNull(UUID.fromString(request.getAssignedAdminId()))
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.getAssignedAdminId()));
            ticket.setAssignedAdmin(assignedAdmin);
        } else if (ticket.getAssignedAdmin() == null) {
            ticket.setAssignedAdmin(admin);
        }

        if (request.getStatus() == SupportTicketStatus.RESOLVED || request.getStatus() == SupportTicketStatus.CLOSED) {
            ticket.setResolvedAt(Instant.now());
        }

        SupportTicket saved = supportTicketRepository.save(ticket);

        auditLogger.log(AuditAction.UPDATE, "SUPPORT_TICKET", saved.getId(),
                "Support ticket status updated to " + request.getStatus(), admin.getId());

        notificationService.sendNotification(NotificationEvent.builder()
                .recipientId(ticket.getUser().getId())
                .phoneNumber(ticket.getUser().getPhoneNumber())
                .type(NotificationType.SUPPORT_TICKET_UPDATED)
                .title("Support Ticket #" + ticket.getTicketNumber() + " Updated")
                .body("Your support ticket status has been updated to: " + request.getStatus())
                .data(Map.of("ticketId", ticket.getId().toString(), "status", request.getStatus().name()))
                .build());

        return toDetailResponse(saved);
    }

    /**
     * Admin endpoint: Retrieve Support Ticket metrics & 12h SLA breach statistics.
     */
    @Transactional(readOnly = true)
    public SupportTicketResponse.SupportTicketStatsResponse adminGetStats() {
        long open = supportTicketRepository.countByStatusAndDeletedAtIsNull(SupportTicketStatus.OPEN);
        long inProgress = supportTicketRepository.countByStatusAndDeletedAtIsNull(SupportTicketStatus.IN_PROGRESS);
        long waiting = supportTicketRepository.countByStatusAndDeletedAtIsNull(SupportTicketStatus.WAITING_FOR_USER);
        long resolved = supportTicketRepository.countByStatusAndDeletedAtIsNull(SupportTicketStatus.RESOLVED);
        long closed = supportTicketRepository.countByStatusAndDeletedAtIsNull(SupportTicketStatus.CLOSED);

        long breachedSla = supportTicketRepository.countBreachedSla(
                List.of(SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS, SupportTicketStatus.WAITING_FOR_USER),
                Instant.now()
        );

        return SupportTicketResponse.SupportTicketStatsResponse.builder()
                .openTicketsCount(open)
                .inProgressTicketsCount(inProgress)
                .waitingForUserTicketsCount(waiting)
                .resolvedTicketsCount(resolved)
                .closedTicketsCount(closed)
                .totalBreachedSlaCount(breachedSla)
                .slaWindowHours(SLA_WINDOW_HOURS)
                .build();
    }

    // --- Private Helper Methods ---

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return userRepository.findByIdAndDeletedAtIsNull(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", userDetails.getId().toString()));
    }

    private String generateTicketNumber() {
        String dateStr = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC")).format(Instant.now());
        int randomNum = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "SUP-" + dateStr + "-" + randomNum;
    }

    private SupportTicketResponse.SupportTicketSummaryResponse toSummaryResponse(SupportTicket ticket) {
        Instant now = Instant.now();
        boolean breached = (ticket.getStatus() == SupportTicketStatus.OPEN || ticket.getStatus() == SupportTicketStatus.IN_PROGRESS || ticket.getStatus() == SupportTicketStatus.WAITING_FOR_USER)
                && ticket.getTargetSlaDeadline() != null && ticket.getTargetSlaDeadline().isBefore(now);

        return SupportTicketResponse.SupportTicketSummaryResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .userId(ticket.getUser().getId())
                .userName(ticket.getUser().getFullName())
                .subject(ticket.getSubject())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .targetSlaDeadline(ticket.getTargetSlaDeadline())
                .isSlaBreached(breached)
                .createdAt(ticket.getCreatedAt())
                .build();
    }

    private SupportTicketResponse.SupportTicketDetailResponse toDetailResponse(SupportTicket ticket) {
        Instant now = Instant.now();
        boolean breached = (ticket.getStatus() == SupportTicketStatus.OPEN || ticket.getStatus() == SupportTicketStatus.IN_PROGRESS || ticket.getStatus() == SupportTicketStatus.WAITING_FOR_USER)
                && ticket.getTargetSlaDeadline() != null && ticket.getTargetSlaDeadline().isBefore(now);

        List<SupportMessage> messages = supportMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId());
        List<SupportTicketResponse.SupportMessageDetailResponse> messageResponses = messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());

        return SupportTicketResponse.SupportTicketDetailResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .userId(ticket.getUser().getId())
                .userName(ticket.getUser().getFullName())
                .userEmail(ticket.getUser().getEmail())
                .userPhone(ticket.getUser().getPhoneNumber())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .targetSlaDeadline(ticket.getTargetSlaDeadline())
                .isSlaBreached(breached)
                .assignedAdminId(ticket.getAssignedAdmin() != null ? ticket.getAssignedAdmin().getId() : null)
                .assignedAdminName(ticket.getAssignedAdmin() != null ? ticket.getAssignedAdmin().getFullName() : null)
                .adminNotes(ticket.getAdminNotes())
                .attachmentUrl(ticket.getAttachmentUrl())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .messages(messageResponses)
                .build();
    }

    private SupportTicketResponse.SupportMessageDetailResponse toMessageResponse(SupportMessage message) {
        return SupportTicketResponse.SupportMessageDetailResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .senderRole(message.getSender().getRole().name())
                .message(message.getMessage())
                .attachmentUrl(message.getAttachmentUrl())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
