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

    @Value("${FULFILMENT_SERVICE_URL:http://localhost:8083}")
    private String fulfilmentServiceUrl;

    public GatewayConfig(CorrelationIdFilter correlationIdFilter,
                         RateLimitFilter rateLimitFilter) {
        this.correlationIdFilter = correlationIdFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // ── Business routes ───────────────────────────────────────

                .route("partner-service", r -> r
                        .path("/partners/**")
                        .filters(f -> f
                                .filter(correlationIdFilter)
                                .filter(rateLimitFilter))
                        .uri(partnerServiceUrl))

                .route("order-service", r -> r
                        .path("/orders/**")
                        .filters(f -> f
                                .filter(correlationIdFilter)
                                .filter(rateLimitFilter))
                        .uri(orderServiceUrl))

                // ── Swagger API docs routes ────────────────────────────────

                .route("partner-service-docs", r -> r
                        .path("/partner-service/v3/api-docs/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(partnerServiceUrl))

                .route("order-service-docs", r -> r
                        .path("/order-service/v3/api-docs/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(orderServiceUrl))

                .route("fulfilment-service-docs", r -> r
                        .path("/fulfilment-service/v3/api-docs/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(fulfilmentServiceUrl))

                .route("swagger-ui-webjars", r -> r
                        .path("/webjars/**")
                        .uri("http://localhost:8090"))

                .build();
    }
}