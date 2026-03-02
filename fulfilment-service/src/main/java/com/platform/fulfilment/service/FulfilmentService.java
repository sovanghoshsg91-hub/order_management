package com.platform.fulfilment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.fulfilment.entity.Fulfilment;
import com.platform.fulfilment.repository.FulfilmentRepository;
import com.platform.shared.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FulfilmentService {

    private final FulfilmentRepository fulfilmentRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PROCESSED_KEY = "fulfilment:processed:%s";

    public void processOrder(OrderCreatedEvent event) {
        String orderId = event.getOrderId();
        String redisKey = String.format(PROCESSED_KEY, orderId);

        // Idempotency check via Redis
        // If already processed → skip (Kafka may redeliver messages)
        Boolean alreadyProcessed = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(alreadyProcessed)) {
            log.warn("Duplicate event skipped: orderId={} correlationId={}",
                    orderId, event.getCorrelationId());
            return;
        }

        log.info("Processing order: orderId={} partnerId={} correlationId={}",
                orderId, event.getPartnerId(), event.getCorrelationId());

        Instant now = Instant.now();

        // Save fulfilment record
        Fulfilment fulfilment = Fulfilment.builder()
                .fulfilmentId(UUID.randomUUID().toString())
                .orderId(orderId)
                .partnerId(event.getPartnerId())
                .orderType(event.getOrderType())
                .status("COMPLETED")
                .payload(toJson(event.getPayload()))
                .correlationId(event.getCorrelationId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        fulfilmentRepository.save(fulfilment);

        // Mark as processed in Redis (24h TTL)
        // Prevents duplicate processing if Kafka redelivers
        redisTemplate.opsForValue().set(redisKey, "PROCESSED", 24, TimeUnit.HOURS);

        log.info("Fulfilment completed: fulfilmentId={} orderId={} correlationId={}",
                fulfilment.getFulfilmentId(), orderId, event.getCorrelationId());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload", e);
            return "{}";
        }
    }
}