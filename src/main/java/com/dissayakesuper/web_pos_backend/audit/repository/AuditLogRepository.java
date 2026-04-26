package com.dissayakesuper.web_pos_backend.audit.repository;

import com.dissayakesuper.web_pos_backend.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
