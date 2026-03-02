package com.platform.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @DynamoDbSecondaryPartitionKey(indexNames = "partnerId-createdAt-index")
    public String getPartnerId() {
        return partnerId;
    }
}