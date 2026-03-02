package com.platform.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerRevokedEvent {

    private String eventId;
    private String partnerId;
    private String revokedBy;
    private String correlationId;
    private Instant occurredAt;
}