package com.makershub.service;

import com.makershub.dto.request.FactoryRequest;
import com.makershub.entity.Factory;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
import com.makershub.enums.NotificationType;
import com.makershub.enums.UserRole;
import com.makershub.enums.VerificationStatus;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.audit.AuditLogger;
import com.makershub.notification.NotificationEvent;
import com.makershub.notification.NotificationService;
import com.makershub.repository.FactoryRepository;
import com.makershub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final FactoryRepository factoryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public Page<Factory> getVerificationQueue(VerificationStatus status, Pageable pageable) {
        Pageable safePageable = com.makershub.util.PageableUtils.sanitize(pageable);
        if (status != null) {
            return factoryRepository.findByVerificationStatus(status, safePageable);
        }
        return factoryRepository.findAll(safePageable);
    }

    @Transactional
    public Factory reviewFactoryVerification(UUID factoryId, FactoryRequest.VerificationReviewRequest request) {
        // Soft-delete-aware lookup
        Factory factory = factoryRepository.findByIdAndDeletedAtIsNull(factoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Factory", factoryId.toString()));
        VerificationStatus oldStatus = factory.getVerificationStatus();
        factory.setVerificationStatus(request.getStatus());
        factory.setVerificationNotes(request.getNotes());
        Factory saved = factoryRepository.save(factory);

        User user = factory.getUser();
        if (request.getStatus() == VerificationStatus.VERIFIED) {
            user.setIsVerified(true);
            user.setRole(UserRole.FACTORY_OWNER);
            userRepository.save(user);
        }
        auditLogger.log(AuditAction.VERIFY, "FACTORY", saved.getId(), oldStatus.name(), saved.getVerificationStatus().name());

        // Notify factory owner of verification decision
        String title = request.getStatus() == VerificationStatus.VERIFIED ? "Factory Verified" : "Verification Update";
        String body = request.getStatus() == VerificationStatus.VERIFIED
                ? "Congratulations! Your factory has been verified on MakersHub."
                : "Your factory verification status has been updated to: " + request.getStatus()
                + (request.getNotes() != null ? ". Notes: " + request.getNotes() : ".");

        notificationService.sendNotification(NotificationEvent.builder()
                .recipientId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .type(NotificationType.ORDER_STATUS_UPDATE)
                .title(title)
                .body(body)
                .build());

        return saved;
    }

    @Transactional
    public User suspendUser(UUID userId, String reason) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        user.setIsActive(false);
        // H-13: Do NOT clear isVerified on suspension — the user is still verified,
        // just not allowed to log in. Verified status is about identity, not activity.
        User saved = userRepository.save(user);
        auditLogger.log(AuditAction.ADMIN_ACTION, "USER", saved.getId(), "ACTIVE", "SUSPENDED: " + reason);
        return saved;
    }

    /**
     * L-11: Re-activate a previously suspended user.
     */
    @Transactional
    public User unsuspendUser(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        if (user.getIsActive()) {
            throw new BusinessException("User is not suspended", HttpStatus.CONFLICT, "USER_NOT_SUSPENDED");
        }
        user.setIsActive(true);
        User saved = userRepository.save(user);
        auditLogger.log(AuditAction.ADMIN_ACTION, "USER", saved.getId(), "SUSPENDED", "ACTIVE");
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<User> getActiveUsers(int activeMinutesWindow, Pageable pageable) {
        java.time.Instant threshold = java.time.Instant.now().minus(java.time.Duration.ofMinutes(activeMinutesWindow));
        return userRepository.findByLastActiveAtAfterAndDeletedAtIsNullOrderByLastActiveAtDesc(threshold, com.makershub.util.PageableUtils.sanitize(pageable));
    }

    @Transactional(readOnly = true)
    public Page<User> getUsersList(com.makershub.enums.UserRole role, Boolean isActive, String search, Pageable pageable) {
        return userRepository.findAllForAdmin(role, isActive, search, com.makershub.util.PageableUtils.sanitize(pageable));
    }

    @Transactional(readOnly = true)
    public java.util.List<User> getAllUsers() {
        return userRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc();
    }
}
