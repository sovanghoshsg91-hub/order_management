package com.platform.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class IdempotencyKey {

    private String partnerId;
    private String idempotencyKey;
    private String requestHash;   // MD5 of request body
    private String orderId;       // the order created for this key
    private Instant createdAt;
    private long expiresAt;       // TTL — epoch seconds, DynamoDB deletes after 24h

    @DynamoDbPartitionKey
    public String getPartnerId() {
        return partnerId;
    }

    @DynamoDbSortKey
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}