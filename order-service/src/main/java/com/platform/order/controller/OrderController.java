package com.platform.order.controller;

import com.platform.order.dto.OrderListResponse;
import com.platform.order.service.OrderService;
import com.platform.shared.dto.OrderRequest;
import com.platform.shared.dto.OrderResponse;
import com.platform.shared.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "PARTNER only endpoints")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order (idempotent)")
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Partner-Id") String partnerId,
            @Valid @RequestBody OrderRequest request) {

        if (partnerId == null || partnerId.isBlank()) {
            log.warn("JWT missing custom:partnerId claim. sub={}",
                    jwt.getSubject());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ErrorResponse.builder()
                            .errorCode("PARTNER_ID_MISSING")
                            .message("Your account is not linked to a partner profile. " +
                                    "Contact admin.")
                            .build()
            );
        }

        log.info("Creating order: partnerId={} idempotencyKey={}",
                partnerId, idempotencyKey);

        OrderResponse response = orderService.createOrder(
                partnerId, idempotencyKey, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Value("${app.version:1.0.0-local}")
    private String appVersion;

    @GetMapping
    @Operation(summary = "List orders with cursor-based pagination")
    public ResponseEntity<?> listOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Partner-Id") String partnerId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String cursor) {

        if (partnerId == null || partnerId.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ErrorResponse.builder()
                            .errorCode("PARTNER_ID_MISSING")
                            .message("Your account is not linked to a partner profile.")
                            .build()
            );
        }

        log.info("Listing orders: partnerId={} limit={} cursor={}",
                partnerId, limit, cursor);
        OrderListResponse response = orderService.listOrders(partnerId, limit, cursor);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<?> getOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Partner-Id") String partnerId,
            @PathVariable String orderId) {

        if (partnerId == null || partnerId.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ErrorResponse.builder()
                            .errorCode("PARTNER_ID_MISSING")
                            .message("Your account is not linked to a partner profile.")
                            .build()
            );
        }

        log.info("Getting order: orderId={} partnerId={}", orderId, partnerId);
        OrderResponse response = orderService.getOrder(orderId, partnerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}