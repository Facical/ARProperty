package com.arproperty.controller;

/** 헬스체크 - GET /health */
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "success",
                "data", Map.of("service", "arproperty-backend",
                        "version", "0.1.0")
        );
    }
}
