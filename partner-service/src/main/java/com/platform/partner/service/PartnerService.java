package com.platform.partner.service;

import com.platform.partner.model.Partner;
import com.platform.partner.repository.PartnerRepository;
import com.platform.shared.dto.PartnerRequest;
import com.platform.shared.dto.PartnerResponse;
import com.platform.shared.event.PartnerRevokedEvent;
import com.platform.shared.exception.PartnerNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PARTNER_STATUS_KEY = "partner:%s:status";
    private static final String TOPIC_PARTNER_REVOKED = "partner.revoked";

    public PartnerResponse createPartner(PartnerRequest request) {
        String partnerId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        Partner partner = Partner.builder()
                .partnerId(partnerId)
                .name(request.getName())
                .status("ACTIVE")
                .rateLimit(request.getRateLimit())
                .dailyQuota(request.getDailyQuota())
                .createdAt(now)
                .updatedAt(now)
                .build();

        partnerRepository.save(partner);

        // Cache status in Redis for fast lookup (24h TTL)
        String redisKey = String.format(PARTNER_STATUS_KEY, partnerId);
        redisTemplate.opsForValue().set(redisKey, "ACTIVE", 24, TimeUnit.HOURS);

        log.info("Partner created: partnerId={}", partnerId);
        return toResponse(partner);
    }

    public PartnerResponse getPartner(String partnerId) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException(partnerId));
        return toResponse(partner);
    }

    public PartnerResponse revokePartner(String partnerId) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException(partnerId));

        // 1. Update DynamoDB
        partner.setStatus("REVOKED");
        partner.setUpdatedAt(Instant.now());
        partnerRepository.update(partner);

        // 2. Update Redis immediately — kill switch activated
        String redisKey = String.format(PARTNER_STATUS_KEY, partnerId);
        redisTemplate.opsForValue().set(redisKey, "REVOKED", 24, TimeUnit.HOURS);

        // 3. Publish event to Kafka
        PartnerRevokedEvent event = PartnerRevokedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .partnerId(partnerId)
                .revokedBy(MDC.get("userId") != null ? MDC.get("userId") : "admin")
                .correlationId(MDC.get("correlationId"))
                .occurredAt(Instant.now())
                .build();

        kafkaTemplate.send(TOPIC_PARTNER_REVOKED, partnerId, event);

        log.info("Partner revoked: partnerId={}", partnerId);
        return toResponse(partner);
    }

    private PartnerResponse toResponse(Partner partner) {
        return PartnerResponse.builder()
                .partnerId(partner.getPartnerId())
                .name(partner.getName())
                .status(partner.getStatus())
                .rateLimit(partner.getRateLimit())
                .dailyQuota(partner.getDailyQuota())
                .createdAt(partner.getCreatedAt())
                .build();
    }
}