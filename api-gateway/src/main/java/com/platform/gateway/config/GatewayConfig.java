package com.platform.gateway.config;

import com.platform.gateway.filter.CorrelationIdFilter;
import com.platform.gateway.filter.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final CorrelationIdFilter correlationIdFilter;
    private final RateLimitFilter rateLimitFilter;

    @Value("${PARTNER_SERVICE_URL:http://localhost:8081}")
    private String partnerServiceUrl;

    @Value("${ORDER_SERVICE_URL:http://localhost:8082}")
    private String orderServiceUrl;

    public GatewayConfig(CorrelationIdFilter correlationIdFilter,
                         RateLimitFilter rateLimitFilter) {
        this.correlationIdFilter = correlationIdFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // partner-service routes
                .route("partner-service", r -> r
                        .path("/partners/**")
                        .filters(f -> f
                                .filter(correlationIdFilter)
                                .filter(rateLimitFilter))
                        .uri(partnerServiceUrl))

                // order-service — single instance in ECS
                .route("order-service-1", r -> r
                        .path("/orders/**")
                        .filters(f -> f
                                .filter(correlationIdFilter)
                                .filter(rateLimitFilter))
                        .uri(orderServiceUrl))

                .build();
    }
}