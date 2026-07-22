package com.makershub.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.makershub.entity.AuditLog;
import com.makershub.enums.AuditAction;
import com.makershub.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogSaver {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    @SneakyThrows
    public void saveAsync(AuditAction action, String entityType, UUID entityId, Object oldValues, Object newValues,
                          UUID userId, String ipAddress, String userAgent) {
        AuditLog logEntry = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValues(oldValues != null ? objectMapper.writeValueAsString(oldValues) : null)
                .newValues(newValues != null ? objectMapper.writeValueAsString(newValues) : null)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        auditLogRepository.save(logEntry);
    }
}
