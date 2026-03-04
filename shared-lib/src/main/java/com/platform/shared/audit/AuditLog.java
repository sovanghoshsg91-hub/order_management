package com.platform.shared.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class AuditLog {

    private String auditId;        // UUID — PK
    private String timestamp;      // ISO-8601 — SK
    private String eventType;      // ORDER_CREATED, PARTNER_REVOKED etc
    private String resourceType;   // ORDER, PARTNER, FULFILMENT
    private String resourceId;     // orderId, partnerId — NO name/email
    private String changedBy;      // userId (JWT sub) — NOT email/name
    private String correlationId;  // trace across services
    private String serviceName;    // order-service, partner-service
    private String oldStatus;      // status before change
    private String newStatus;      // status after change
    private String result;         // SUCCESS or FAILURE
    private String failureReason;  // if FAILURE — no PII

    @DynamoDbPartitionKey
    public String getAuditId() { return auditId; }

    @DynamoDbSortKey
    public String getTimestamp() { return timestamp; }
}
