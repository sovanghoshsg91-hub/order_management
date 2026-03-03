package com.platform.gateway.config;

import com.platform.gateway.filter.CorrelationIdFilter;
import com.platform.gateway.filter.RateLimitFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final CorrelationIdFilter correlationIdFilter;
    private final RateLimitFilter rateLimitFilter;

    public GatewayConfig(CorrelationIdFilter correlationIdFilter,
                         RateLimitFilter rateLimitFilter) {
        this.correlationIdFilter = correlationIdFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // partner-service routes — ADMIN only
                .route("partner-service", r -> r
                        .path("/partners/**")
                        .filters(f -> f
                                .filter(correlationIdFilter)
                                .filter(rateLimitFilter))
                        .uri("http://localhost:8081"))

                // order-service routes — PARTNER only
                .route("order-service", r -> r
                        .path("/orders/**")
                        .filters(f -> f
                                .filter(correlationIdFilter)
                                .filter(rateLimitFilter))
                        .uri("http://localhost:8082"))

                .build();
    }
}