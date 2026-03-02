package com.platform.fulfilment.repository;

import com.platform.fulfilment.entity.Fulfilment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FulfilmentRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private DynamoDbTable<Fulfilment> table() {
        return dynamoDbEnhancedClient.table("Fulfilments",
                TableSchema.fromBean(Fulfilment.class));
    }

    public void save(Fulfilment fulfilment) {
        log.info("Saving fulfilment: {}", fulfilment.getFulfilmentId());
        table().putItem(fulfilment);
    }

    public Optional<Fulfilment> findByOrderId(String orderId) {
        // Scan is acceptable here since this is for deduplication check only
        // In production: add orderId-index GSI
        return table().scan().items().stream()
                .filter(f -> orderId.equals(f.getOrderId()))
                .findFirst();
    }
}