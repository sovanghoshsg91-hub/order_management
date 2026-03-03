package com.platform.order.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@DynamoDbBean
public class Order {

    private String orderId;
    private String partnerId;
    private String status;
    private String orderType;
    private String payload;
    private String correlationId;
    private Instant createdAt;
    private Instant updatedAt;

    @DynamoDbPartitionKey
    public String getOrderId() {
        return orderId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"partnerId-index"})
    public String getPartnerId() {
        return partnerId;
    }
}