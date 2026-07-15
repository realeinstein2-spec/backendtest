package com.makershub.service;

import com.makershub.dto.request.FactoryRequest;
import com.makershub.entity.Factory;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
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
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<Factory> getVerificationQueue(VerificationStatus status, Pageable pageable) {
        if (status != null) {
            return factoryRepository.findByVerificationStatus(status, pageable);
        }
        return factoryRepository.findAll(pageable);
    }

    @Transactional
    public Factory reviewFactoryVerification(UUID factoryId, FactoryRequest.VerificationReviewRequest request) {
        // Note: FactoryRepository does not expose findByIdAndDeletedAtIsNull; using findById
        Factory factory = factoryRepository.findById(factoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Factory", factoryId.toString()));
        VerificationStatus oldStatus = factory.getVerificationStatus();
        factory.setVerificationStatus(request.getStatus());
        factory.setVerificationNotes(request.getNotes());
        Factory saved = factoryFactory(factory);

        User user = factory.getUser();
        if (request.getStatus() == VerificationStatus.VERIFIED) {
            user.setIsVerified(true);
            user.setRole(UserRole.FACTORY_OWNER);
            userRepository.save(user);
        }
        auditLogger.log(AuditAction.VERIFY, "FACTORY", saved.getId(), oldStatus.name(), saved.getVerificationStatus().name());

        // Notify factory owner of verification decision
        NotificationEvent notif = NotificationEvent.builder()
                .recipientId(factory.getUser().getId())
                .phoneNumber(factory.getUser().getPhoneNumber())
                .type(com.makershub.enums.NotificationType.VERIFICATION_STATUS)
                .title(request.getStatus() == VerificationStatus.VERIFIED ? "Factory Verified" : "Verification Update")
                .body(request.getStatus() == VerificationStatus.VERIFIED
                        ? "Congratulations! Your factory has been verified on MakersHub."
                        : "Your factory verification status has been updated: " + request.getStatus())
                .build();
        notificationService.sendNotification(notif);

        return saved;
    }

    // H-13/L-15: Suspension only deactivates; verification status is preserved
    @Transactional
    public User suspendUser(UUID userId, String reason) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        user.setIsActive(false);
        User saved = userRepository.save(user);
        auditLogger.log(AuditAction.ADMIN_ACTION, "USER", saved.getId(), "ACTIVE", "SUSPENDED: " + reason);
        return saved;
    }

    // L-11: Allow admins to re-activate a previously suspended user
    @Transactional
    public User unsuspendUser(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        user.setIsActive(true);
        User saved = userRepository.save(user);
        auditLogger.log(AuditAction.ADMIN_ACTION, "USER", saved.getId(), "SUSPENDED", "ACTIVE");
        return saved;
    }

    /** Thin helper so the save call is not duplicated. */
    private Factory factoryFactory(Factory factory) {
        return factoryRepository.save(factory);
    }
}
