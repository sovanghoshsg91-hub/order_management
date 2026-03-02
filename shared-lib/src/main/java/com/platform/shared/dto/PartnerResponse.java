package com.platform.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerResponse {

    private String partnerId;
    private String name;
    private String status;
    private Integer rateLimit;
    private Integer dailyQuota;
    private Instant createdAt;
}