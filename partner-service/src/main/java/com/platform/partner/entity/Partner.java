package com.platform.partner.entity;

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
public class Partner {

    private String partnerId;
    private String name;
    private String status;       // ACTIVE or REVOKED
    private Integer rateLimit;   // requests per second
    private Integer dailyQuota;  // requests per day
    private Instant createdAt;
    private Instant updatedAt;

    @DynamoDbPartitionKey
    public String getPartnerId() {
        return partnerId;
    }
}