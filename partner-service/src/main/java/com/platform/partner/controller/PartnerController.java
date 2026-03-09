package com.platform.partner.controller;

import com.platform.partner.service.PartnerService;
import com.platform.shared.dto.PartnerRequest;
import com.platform.shared.dto.PartnerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/partners")
@RequiredArgsConstructor
@Tag(name = "Partner Management", description = "ADMIN only endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PartnerController {

    private final PartnerService partnerService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasRole('ADMIN')")
    @Operation(summary = "Create a new partner")
    public ResponseEntity<PartnerResponse> createPartner(
            @Valid @RequestBody PartnerRequest request) {
        log.info("Creating partner: name={}", request.getName());
        PartnerResponse response = partnerService.createPartner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{partnerId}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasRole('ADMIN')")
    @Operation(summary = "Get partner by ID")
    public ResponseEntity<PartnerResponse> getPartner(
            @PathVariable String partnerId) {
        log.info("Getting partner: partnerId={}", partnerId);
        PartnerResponse response = partnerService.getPartner(partnerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{partnerId}/revoke")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasRole('ADMIN')")
    @Operation(summary = "Revoke partner access immediately")
    public ResponseEntity<PartnerResponse> revokePartner(
            @PathVariable String partnerId) {
        log.info("Revoking partner: partnerId={}", partnerId);
        PartnerResponse response = partnerService.revokePartner(partnerId);
        return ResponseEntity.ok(response);
    }

    // Internal endpoint for service-to-service calls
    // No @PreAuthorize — any valid JWT can call this
    @GetMapping("/internal/{partnerId}/status")
    public ResponseEntity<String> getPartnerStatus(
            @PathVariable String partnerId) {
        String status = partnerService.getPartnerStatus(partnerId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}