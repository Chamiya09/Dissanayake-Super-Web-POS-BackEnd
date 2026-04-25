package com.dissayakesuper.web_pos_backend.audit.repository;

import com.dissayakesuper.web_pos_backend.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

	java.util.List<AuditLog> findTop8ByOrderByTimestampDesc();
}
 