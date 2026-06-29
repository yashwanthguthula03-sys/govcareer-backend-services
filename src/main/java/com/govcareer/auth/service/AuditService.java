package com.govcareer.auth.service;

import com.govcareer.auth.entity.AuthAudit;
import com.govcareer.auth.entity.AuditEventType;
import com.govcareer.auth.repository.AuthAuditRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;

@Service
public class AuditService {

    private final AuthAuditRepository auditRepository;

    public AuditService(AuthAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Async
    public void logEvent(UUID userId, AuditEventType eventType) {
        String correlationId = MDC.get("correlationId");
        String clientIp = MDC.get("clientIp");
        String userAgent = MDC.get("userAgent");

        AuthAudit audit = AuthAudit.builder()
                .userId(userId)
                .eventType(eventType)
                .timestamp(Instant.now())
                .clientIp(clientIp)
                .userAgent(userAgent)
                .correlationId(correlationId)
                .build();

        auditRepository.save(audit);
    }
}
