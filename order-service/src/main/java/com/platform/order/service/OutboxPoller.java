package com.platform.order.service;

import com.platform.order.entity.OutboxEvent;
import com.platform.order.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 500)
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents(10);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("OutboxPoller: found {} pending events",
                pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            boolean claimed = outboxRepository.claimEvent(event.getEventId());
            if (!claimed) {
                log.info("Event already claimed by another instance, skipping: {}", event.getEventId());
                continue;
            }
            try {
                kafkaTemplate.send(event.getEventType(),
                                event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish: " +
                                                "eventId={} error={}",
                                        event.getEventId(),
                                        ex.getMessage());
                            } else {
                                markPublished(event);
                            }
                        });

            } catch (Exception e) {
                log.error("OutboxPoller error: eventId={}",
                        event.getEventId(), e);
            }
        }
    }

    private void markPublished(OutboxEvent event) {
        try {
            event.setStatus("PUBLISHED");
            event.setPublishedAt(Instant.now());
            // @DynamoDbVersionAttribute automatically adds:
            // condition: version = currentVersion
            // increment: version + 1
            outboxRepository.update(event);
            log.info("Event published: eventId={} topic={}",
                    event.getEventId(), event.getEventType());

        } catch (DynamoDbException e) {
            if (e.getMessage() != null && e.getMessage()
                    .contains("ConditionalCheckFailedException")) {
                // Another poller instance already marked it
                // published — safe to ignore ✅
                log.info("Event {} already published by " +
                                "another instance — skipping",
                        event.getEventId());
            } else {
                log.error("Failed to mark event published: " +
                                "eventId={} error={}",
                        event.getEventId(), e.getMessage());
            }
        }
    }
}