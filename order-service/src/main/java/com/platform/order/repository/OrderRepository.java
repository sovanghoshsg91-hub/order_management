package com.platform.order.repository;

import com.platform.order.entity.Order;
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

    public Optional<Order> findById(String orderId) {
        Order order = table().getItem(
                Key.builder().partitionValue(orderId).build());
        return Optional.ofNullable(order);
    }
}