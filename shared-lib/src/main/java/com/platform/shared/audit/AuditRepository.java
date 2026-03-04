package com.platform.shared.audit;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Slf4j
public class AuditRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    public AuditRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    }

    private DynamoDbTable<AuditLog> table() {
        return dynamoDbEnhancedClient.table("AuditLogs",
                TableSchema.fromBean(AuditLog.class));
    }

    public void save(AuditLog auditLog) {
        try {
            table().putItem(auditLog);
            log.debug("Audit log saved: auditId={} eventType={}",
                    auditLog.getAuditId(), auditLog.getEventType());
        } catch (Exception e) {
            // Audit failure must NEVER affect business logic
            log.error("Failed to save audit log: eventType={} resourceId={} error={}",
                    auditLog.getEventType(),
                    auditLog.getResourceId(),
                    e.getMessage());
        }
    }
}