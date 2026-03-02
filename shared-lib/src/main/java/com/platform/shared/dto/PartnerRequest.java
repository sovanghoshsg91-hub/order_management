package com.platform.shared.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerRequest {

    @NotBlank(message = "Partner name is required")
    private String name;

    @NotNull(message = "Rate limit is required")
    @Min(value = 1, message = "Rate limit must be at least 1")
    private Integer rateLimit;

    @NotNull(message = "Daily quota is required")
    @Min(value = 1, message = "Daily quota must be at least 1")
    private Integer dailyQuota;
}