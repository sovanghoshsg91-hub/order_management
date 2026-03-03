package com.platform.order.repository;

import com.platform.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private DynamoDbTable<Order> table() {
        return dynamoDbEnhancedClient.table("Orders",
                TableSchema.fromBean(Order.class));
    }

    public Order save(Order order) {
        log.info("Saving order: {}", order.getOrderId());
        table().putItem(order);
        return order;
    }

    public DynamoDbIndex<Order> getIndex() {
        return table().index("partnerId-index");
    }

    public Optional<Order> findById(String orderId) {
        Order order = table().getItem(
                Key.builder().partitionValue(orderId).build());
        return Optional.ofNullable(order);
    }
}