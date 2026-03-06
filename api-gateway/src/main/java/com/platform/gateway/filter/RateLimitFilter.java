package com.platform.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

@Component
public class RateLimitFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int RATE_LIMIT_PER_SECOND = 100;
    private static final int DAILY_QUOTA = 10000;

    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (Jwt) ctx.getAuthentication().getPrincipal())
                .flatMap(jwt -> {
                    String partnerId = jwt.getSubject();

                    return checkRateLimit(partnerId, exchange)
                            .flatMap(rateLimitOk -> {
                                if (!rateLimitOk) {
                                    return rejectRequest(exchange,
                                            HttpStatus.TOO_MANY_REQUESTS,
                                            "RATE_LIMIT_EXCEEDED",
                                            "Rate limit exceeded: max " + RATE_LIMIT_PER_SECOND + " requests/second");
                                }
                                return checkDailyQuota(partnerId, exchange)
                                        .flatMap(quotaOk -> {
                                            if (!quotaOk) {
                                                return rejectRequest(exchange,
                                                        HttpStatus.TOO_MANY_REQUESTS,
                                                        "DAILY_QUOTA_EXCEEDED",
                                                        "Daily quota exceeded: max " + DAILY_QUOTA + " requests/day");
                                            }
                                            return chain.filter(exchange);
                                        });
                            });
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<Boolean> checkRateLimit(String partnerId, ServerWebExchange exchange) {
        String key = "rate:" + partnerId;
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        // First request this second — set 1s expiry
                        return redisTemplate.expire(key, Duration.ofSeconds(1))
                                .thenReturn(true);
                    }
                    boolean allowed = count <= RATE_LIMIT_PER_SECOND;
                    if (!allowed) {
                        log.warn("Rate limit exceeded: partnerId={} count={}", partnerId, count);
                    }
                    return Mono.just(allowed);
                });
    }

    private Mono<Boolean> checkDailyQuota(String partnerId, ServerWebExchange exchange) {
        String today = LocalDate.now().toString();
        String key = "quota:" + partnerId + ":" + today;
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        // First request today — set 24h expiry
                        return redisTemplate.expire(key, Duration.ofHours(24))
                                .thenReturn(true);
                    }
                    boolean allowed = count <= DAILY_QUOTA;
                    if (!allowed) {
                        log.warn("Daily quota exceeded: partnerId={} count={}", partnerId, count);
                    }
                    return Mono.just(allowed);
                });
    }

    private Mono<Void> rejectRequest(ServerWebExchange exchange,
                                     HttpStatus status,
                                     String errorCode,
                                     String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"errorCode\":\"%s\",\"message\":\"%s\"}", errorCode, message);
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}