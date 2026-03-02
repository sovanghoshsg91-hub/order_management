package com.platform.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.order.entity.IdempotencyKey;
import com.platform.order.entity.Order;
import com.platform.order.entity.OutboxEvent;
import com.platform.order.repository.IdempotencyRepository;
import com.platform.order.repository.OrderRepository;
import com.platform.order.repository.OutboxRepository;
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

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final OutboxRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PARTNER_STATUS_KEY = "partner:%s:status";

    public OrderResponse createOrder(String partnerId,
                                     String idempotencyKeyHeader,
                                     OrderRequest request) {
        String correlationId = MDC.get("correlationId");

        // Step 1: Kill switch check
        checkPartnerNotRevoked(partnerId);

        // Step 2: Idempotency check
        String requestHash = computeHash(request);
        Optional<IdempotencyKey> existingKey = idempotencyRepository
                .findByPartnerAndKey(partnerId, idempotencyKeyHeader);

        if (existingKey.isPresent()) {
            IdempotencyKey existing = existingKey.get();

            // Same key + same payload = return original response (duplicate request)
            if (existing.getRequestHash().equals(requestHash)) {
                log.info("Duplicate request detected, returning original response. " +
                        "orderId={}", existing.getOrderId());
                return orderRepository.findById(existing.getOrderId())
                        .map(this::toResponse)
                        .orElseThrow(() -> new OrderNotFoundException(existing.getOrderId()));
            }

            // Same key + different payload = conflict!
            log.warn("Idempotency conflict: same key, different payload. key={}",
                    idempotencyKeyHeader);
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
                .payload(toJson(request.getPayload()))
                .correlationId(correlationId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        orderRepository.save(order);
        log.info("Order created: orderId={} partnerId={}", orderId, partnerId);

        // Step 4: Save idempotency key (TTL 24h)
        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .partnerId(partnerId)
                .idempotencyKey(idempotencyKeyHeader)
                .requestHash(requestHash)
                .orderId(orderId)
                .createdAt(now)
                .expiresAt(now.getEpochSecond() + 86400) // 24 hours TTL
                .build();

        idempotencyRepository.save(idempotencyKey);
        log.info("Idempotency key saved: key={}", idempotencyKeyHeader);

        // Step 5: Save outbox event (status=PENDING)
        // Poller picks this up and publishes to Kafka
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
        log.info("Outbox event saved: eventId={}", outboxEvent.getEventId());

        return toResponse(order);
    }

    public OrderResponse getOrder(String orderId, String partnerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
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