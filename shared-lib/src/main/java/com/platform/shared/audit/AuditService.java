package com.platform.shared.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;
    private final String serviceName;

    public void log(String eventType,
                    String resourceType,
                    String resourceId,
                    String changedBy,
                    String correlationId,
                    String oldStatus,
                    String newStatus,
                    String result,
                    String failureReason) {

        AuditLog auditLog = AuditLog.builder()
                .auditId(UUID.randomUUID().toString())
                .timestamp(Instant.now().toString())
                .eventType(eventType)
                .resourceType(resourceType)
                .resourceId(resourceId)        // ID only — no PII ✅
                .changedBy(changedBy)          // userId only — no email ✅
                .correlationId(correlationId)
                .serviceName(serviceName)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .result(result)
                .failureReason(failureReason)  // no PII in reason ✅
                .build();

        auditRepository.save(auditLog);
    }

    // Convenience method for SUCCESS
    public void success(String eventType,
                        String resourceType,
                        String resourceId,
                        String changedBy,
                        String correlationId,
                        String oldStatus,
                        String newStatus) {
        log(eventType, resourceType, resourceId,
                changedBy, correlationId,
                oldStatus, newStatus,
                "SUCCESS", null);
    }

    // Convenience method for FAILURE
    public void failure(String eventType,
                        String resourceType,
                        String resourceId,
                        String changedBy,
                        String correlationId,
                        String failureReason) {
        log(eventType, resourceType, resourceId,
                changedBy, correlationId,
                null, null,
                "FAILURE", failureReason);
    }
}