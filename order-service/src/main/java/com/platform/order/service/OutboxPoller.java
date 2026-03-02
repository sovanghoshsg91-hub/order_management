package com.platform.order.service;

import com.platform.order.entity.OutboxEvent;
import com.platform.order.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Runs every 500ms
    @Scheduled(fixedDelay = 500)
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents(10);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("OutboxPoller: found {} pending events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Publish to Kafka topic
                kafkaTemplate.send(event.getEventType(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish event: eventId={} error={}",
                                        event.getEventId(), ex.getMessage());
                            } else {
                                // Mark as PUBLISHED only after Kafka confirms
                                event.setStatus("PUBLISHED");
                                event.setPublishedAt(Instant.now());
                                outboxRepository.update(event);
                                log.info("Event published: eventId={} topic={}",
                                        event.getEventId(), event.getEventType());
                            }
                        });

            } catch (Exception e) {
                log.error("OutboxPoller error: eventId={}", event.getEventId(), e);
            }
        }
    }
}