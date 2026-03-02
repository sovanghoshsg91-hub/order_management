package com.platform.shared.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreatedEvent {

    private String eventId;
    private String orderId;
    private String partnerId;
    private String orderType;
    private Map<String, Object> payload;
    private String correlationId;
    private Instant occurredAt;
}