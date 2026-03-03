package com.platform.gateway.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;

@RestController
public class SystemController {

    @Value("${app.version:1.0.0-local}")
    private String appVersion;

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/version")
    public ResponseEntity<?> version() {
        return ResponseEntity.ok(Map.of("version", appVersion));
    }
}
