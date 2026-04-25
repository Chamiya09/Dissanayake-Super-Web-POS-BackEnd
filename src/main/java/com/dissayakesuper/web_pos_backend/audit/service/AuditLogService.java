package com.dissayakesuper.web_pos_backend.audit.service;

import com.dissayakesuper.web_pos_backend.audit.entity.AuditLog;
import com.dissayakesuper.web_pos_backend.audit.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(Long userId, String action, String details) {
        AuditLog entry = AuditLog.builder()
                .userId(userId)
                .action(action)
                .details(details)
                .build();
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getLogs(Long userId,
                                  String action,
                                  LocalDateTime from,
                                  LocalDateTime to,
                                  int page,
                                  int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("action")), "%" + action.toLowerCase() + "%"));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), to));
        }

        return auditLogRepository.findAll(spec, pageable);
    }
}
