package com.dissayakesuper.web_pos_backend.audit.service;

import com.dissayakesuper.web_pos_backend.audit.entity.AuditLog;
import com.dissayakesuper.web_pos_backend.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void logAction(Long userId, String action, String details) {
        auditLogRepository.save(AuditLog.builder()
                .userId(userId)
                .action(action)
                .details(details)
                .build());
    }
}
