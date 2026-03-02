package com.platform.order.repository;

import com.platform.order.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

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
}