package com.platform.fulfilment.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.fulfilment.service.FulfilmentService;
import com.platform.shared.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private final FulfilmentService fulfilmentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "order.created",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received Kafka message: topic={} partition={} offset={}",
                topic, partition, offset);

        try {
            // Strip surrounding quotes if double-serialized
            // "\"{ json }\"" → "{ json }"
            String cleanMessage = message;
            if (message.startsWith("\"") && message.endsWith("\"")) {
                cleanMessage = objectMapper.readValue(message, String.class);
            }

            OrderCreatedEvent event = objectMapper.readValue(
                    cleanMessage, OrderCreatedEvent.class);

            log.info("Deserialised event: orderId={} partnerId={} correlationId={}",
                    event.getOrderId(), event.getPartnerId(), event.getCorrelationId());

            fulfilmentService.processOrder(event);

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialise Kafka message: {} error={}",
                    message, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process order event: error={}", e.getMessage(), e);
        }
    }
}