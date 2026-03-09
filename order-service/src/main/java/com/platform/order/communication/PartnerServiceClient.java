package com.platform.order.communication;

import com.platform.shared.dto.PartnerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "partner-service",
        url = "${partner.service.url:http://partner-service.partner-orders.local:8081}"
)
public interface PartnerServiceClient {

    @GetMapping("/internal/partners/{partnerId}/status")
    String getPartnerStatus(@PathVariable("partnerId") String partnerId);
}