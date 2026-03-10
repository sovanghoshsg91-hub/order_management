package com.platform.order.repository;

import com.platform.order.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbClient dynamoDbClient;

    private DynamoDbTable<OutboxEvent> table() {
        return dynamoDbEnhancedClient.table("OutboxEvents",
                TableSchema.fromBean(OutboxEvent.class));
    }

    public OutboxEvent save(OutboxEvent event) {
        table().putItem(event);
        return event;
    }

    public OutboxEvent update(OutboxEvent event) {
        table().updateItem(event);
        return event;
    }

    public List<OutboxEvent> findPendingEvents(int limit) {
        DynamoDbIndex<OutboxEvent> index = table().index("status-index");

        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder()
                        .partitionValue("PENDING")
                        .build());

        return index.query(r -> r
                        .queryConditional(queryConditional)
                        .limit(limit))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    public boolean claimEvent(String eventId) {
        try {
            Map<String, AttributeValue> key = Map.of(
                    "eventId", AttributeValue.builder().s(eventId).build()
            );

            Map<String, AttributeValue> expressionValues = Map.of(
                    ":pending",    AttributeValue.builder().s("PENDING").build(),
                    ":processing", AttributeValue.builder().s("PROCESSING").build()
            );

            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName("OutboxEvents")
                    .key(key)
                    .updateExpression("SET #status = :processing")
                    .conditionExpression("#status = :pending")  // ← atomic check
                    .expressionAttributeNames(Map.of("#status", "status"))
                    .expressionAttributeValues(expressionValues)
                    .build();

            dynamoDbClient.updateItem(request);
            return true; // claimed successfully

        } catch (ConditionalCheckFailedException e) {
            return false; // another instance already claimed it
        }
    }
}