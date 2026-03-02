package com.platform.order.repository;

import com.platform.order.entity.IdempotencyKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IdempotencyRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private DynamoDbTable<IdempotencyKey> table() {
        return dynamoDbEnhancedClient.table("IdempotencyKeys",
                TableSchema.fromBean(IdempotencyKey.class));
    }

    public IdempotencyKey save(IdempotencyKey idempotencyKey) {
        table().putItem(idempotencyKey);
        return idempotencyKey;
    }

    public Optional<IdempotencyKey> findByPartnerAndKey(
            String partnerId, String idempotencyKey) {
        IdempotencyKey item = table().getItem(
                Key.builder()
                        .partitionValue(partnerId)
                        .sortValue(idempotencyKey)
                        .build());
        return Optional.ofNullable(item);
    }
}