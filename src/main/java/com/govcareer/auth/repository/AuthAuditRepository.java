package com.govcareer.auth.repository;

import com.govcareer.auth.entity.AuthAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAuditRepository extends JpaRepository<AuthAudit, Long> {
}
