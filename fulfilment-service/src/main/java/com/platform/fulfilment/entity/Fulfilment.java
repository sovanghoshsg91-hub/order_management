package com.platform.fulfilment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Fulfilment {

    private String fulfilmentId;    // PK
    private String orderId;         // from Kafka event
    private String partnerId;       // from Kafka event
    private String orderType;       // DELIVERY etc
    private String status;          // PROCESSING, COMPLETED, FAILED
    private String payload;         // JSON string
    private String correlationId;
    private Instant createdAt;
    private Instant updatedAt;

    @DynamoDbPartitionKey
    public String getFulfilmentId() { return fulfilmentId; }
}