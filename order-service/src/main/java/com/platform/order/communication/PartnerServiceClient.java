package com.platform.order.communication;

import com.platform.shared.dto.PartnerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// order-service
@FeignClient(
        name = "partner-service",
        url = "${partner.service.url:http://partner-service.partner-orders.local:8082}"
)
public interface PartnerServiceClient {

    @GetMapping("/partners/{partnerId}")
    PartnerResponse getPartner(@PathVariable("partnerId") String partnerId);
}