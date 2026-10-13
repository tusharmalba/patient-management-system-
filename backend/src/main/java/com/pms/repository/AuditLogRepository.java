package com.pms.repository;

import com.pms.entity.AuditLog;
import com.pms.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
    Page<AuditLog> findByPerformedBy(String performedBy, Pageable pageable);
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);
}
