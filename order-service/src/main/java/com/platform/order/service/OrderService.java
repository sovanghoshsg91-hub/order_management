package com.platform.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.order.dto.OrderListResponse;
import com.platform.order.entity.IdempotencyKey;
import com.platform.order.entity.Order;
import com.platform.order.entity.OutboxEvent;
import com.platform.order.repository.IdempotencyRepository;
import com.platform.order.repository.OrderRepository;
import com.platform.order.repository.OutboxRepository;
import com.platform.shared.audit.AuditEventType;
import com.platform.shared.audit.AuditService;
import com.platform.shared.dto.OrderRequest;
import com.platform.shared.dto.OrderResponse;
import com.platform.shared.event.OrderCreatedEvent;
import com.platform.shared.exception.IdempotencyConflictException;
import com.platform.shared.exception.OrderNotFoundException;
import com.platform.shared.exception.PartnerRevokedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final OutboxRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private static final String PARTNER_STATUS_KEY = "partner:%s:status";

    public OrderResponse createOrder(String partnerId,
                                     String idempotencyKeyHeader,
                                     OrderRequest request) {
        String correlationId = MDC.get("correlationId");

        // Step 1: Kill switch check
        try {
            checkPartnerNotRevoked(partnerId);
        } catch (PartnerRevokedException e) {
            // Audit: blocked request — no PII ✅
            auditService.failure(
                    AuditEventType.PARTNER_REVOKED_BLOCKED,
                    "ORDER", null,
                    partnerId, correlationId,
                    "Partner is revoked");
            throw e;
        }

        // Step 2: Idempotency check
        String requestHash = computeHash(request);
        Optional<IdempotencyKey> existingKey = idempotencyRepository
                .findByPartnerAndKey(partnerId, idempotencyKeyHeader);

        if (existingKey.isPresent()) {
            IdempotencyKey existing = existingKey.get();

            if (existing.getRequestHash().equals(requestHash)) {
                log.info("Duplicate request: orderId={}",
                        existing.getOrderId());
                // Audit: duplicate detected — no PII ✅
                auditService.success(
                        AuditEventType.ORDER_DUPLICATE,
                        "ORDER", existing.getOrderId(),
                        partnerId, correlationId,
                        null, "CREATED");
                return orderRepository.findById(existing.getOrderId())
                        .map(this::toResponse)
                        .orElseThrow(() ->
                                new OrderNotFoundException(existing.getOrderId()));
            }

            // Audit: idempotency conflict — no PII ✅
            auditService.failure(
                    AuditEventType.IDEMPOTENCY_CONFLICT,
                    "ORDER", null,
                    partnerId, correlationId,
                    "Idempotency key reused with different payload");
            throw new IdempotencyConflictException(idempotencyKeyHeader);
        }

        // Step 3: Create order
        String orderId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        Order order = Order.builder()
                .orderId(orderId)
                .partnerId(partnerId)
                .status("CREATED")
                .orderType(request.getOrderType())
                .payload(request.getPayload().toString())
                .correlationId(correlationId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        orderRepository.save(order);

        // Step 4: Save idempotency key
        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .partnerId(partnerId)
                .idempotencyKey(idempotencyKeyHeader)
                .requestHash(requestHash)
                .orderId(orderId)
                .createdAt(now)
                .expiresAt(now.getEpochSecond() + 86400)
                .build();
        idempotencyRepository.save(idempotencyKey);

        // Step 5: Save outbox event
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .partnerId(partnerId)
                .orderType(request.getOrderType())
                .payload(request.getPayload())
                .correlationId(correlationId)
                .occurredAt(now)
                .build();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .status("PENDING")
                .eventType("order.created")
                .payload(toJson(event))
                .correlationId(correlationId)
                .createdAt(now)
                .build();
        outboxRepository.save(outboxEvent);

        // Audit: order created — resourceId only, no PII ✅
        auditService.success(
                AuditEventType.ORDER_CREATED,
                "ORDER", orderId,
                partnerId, correlationId,
                null, "CREATED");

        log.info("Order created: orderId={}", orderId);
        return toResponse(order);
    }

    public OrderListResponse listOrders(String partnerId,
                                        int limit,
                                        String cursor) {
        String correlationId = MDC.get("correlationId");

        DynamoDbIndex<Order> index = orderRepository.getIndex();

        QueryEnhancedRequest.Builder queryBuilder =
                QueryEnhancedRequest.builder()
                        .queryConditional(
                                QueryConditional.keyEqualTo(
                                        k -> k.partitionValue(partnerId)))
                        .limit(limit);

        if (cursor != null && !cursor.isBlank()) {
            Map<String, AttributeValue> startKey = new HashMap<>();
            startKey.put("partnerId",
                    AttributeValue.builder().s(partnerId).build());
            startKey.put("orderId",
                    AttributeValue.builder().s(cursor).build());
            queryBuilder.exclusiveStartKey(startKey);
        }

        Page<Order> firstPage = index.query(queryBuilder.build())
                .iterator().next();

        String nextCursor = null;
        if (firstPage.lastEvaluatedKey() != null) {
            AttributeValue lastKey =
                    firstPage.lastEvaluatedKey().get("orderId");
            if (lastKey != null) nextCursor = lastKey.s();
        }

        List<OrderResponse> orders = firstPage.items()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // Audit: orders listed — resourceId=partnerId, no PII ✅
        auditService.success(
                AuditEventType.ORDER_ACCESSED,
                "ORDER_LIST", partnerId,
                partnerId, correlationId,
                null, "LISTED:" + orders.size());

        log.info("Orders listed: partnerId={} count={} cursor={}",
                partnerId, orders.size(), cursor);

        return OrderListResponse.builder()
                .orders(orders)
                .nextCursor(nextCursor)
                .build();
    }

    public OrderResponse getOrder(String orderId, String partnerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Audit: order accessed — no PII ✅
        auditService.success(
                AuditEventType.ORDER_ACCESSED,
                "ORDER", orderId,
                partnerId, MDC.get("correlationId"),
                null, order.getStatus());

        return toResponse(order);
    }

    // Kill switch — check Redis first (fast), DynamoDB is source of truth
    private void checkPartnerNotRevoked(String partnerId) {
        String redisKey = String.format(PARTNER_STATUS_KEY, partnerId);
        String status = redisTemplate.opsForValue().get(redisKey);

        if ("REVOKED".equals(status)) {
            log.warn("Blocked revoked partner: partnerId={}", partnerId);
            throw new PartnerRevokedException(partnerId);
        }

        log.debug("Partner status check passed: partnerId={} status={}",
                partnerId, status);
    }

    private String computeHash(OrderRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(json.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to compute request hash", e);
            return UUID.randomUUID().toString();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .partnerId(order.getPartnerId())
                .status(order.getStatus())
                .orderType(order.getOrderType())
                .createdAt(order.getCreatedAt())
                .correlationId(order.getCorrelationId())
                .build();
    }
}