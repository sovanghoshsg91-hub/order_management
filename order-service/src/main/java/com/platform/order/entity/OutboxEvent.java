package com.platform.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class OutboxEvent {

    private String eventId;
    private String status;       // PENDING or PUBLISHED
    private String eventType;    // order.created
    private String payload;      // JSON string of the event
    private String correlationId;
    private Instant createdAt;
    private Instant publishedAt;
    private Long version;

    @DynamoDbPartitionKey
    public String getEventId() {
        return eventId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "status-index")
    public String getStatus() {
        return status;
    }

    @DynamoDbVersionAttribute
    public Long getVersion() {
        return version;
    }
}