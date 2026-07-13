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

    @Transactional(readOnly = true)
    public Page<Factory> getVerificationQueue(VerificationStatus status, Pageable pageable) {
        if (status != null) {
            return factoryRepository.findByVerificationStatus(status, pageable);
        }
        return factoryRepository.findAll(pageable);
    }

    @Transactional
    public Factory reviewFactoryVerification(UUID factoryId, FactoryRequest.VerificationReviewRequest request) {
        Factory factory = factoryRepository.findById(factoryId)
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
        return saved;
    }

    @Transactional
    public User suspendUser(UUID userId, String reason) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        user.setIsActive(false);
        user.setIsVerified(false);
        User saved = userRepository.save(user);
        auditLogger.log(AuditAction.ADMIN_ACTION, "USER", saved.getId(), "ACTIVE", "SUSPENDED: " + reason);
        return saved;
    }
}
