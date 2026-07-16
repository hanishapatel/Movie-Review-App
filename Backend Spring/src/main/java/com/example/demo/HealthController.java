package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight health check (no DB) — use it with a free uptime pinger
 * (e.g. UptimeRobot) to keep the free instance from spinning down.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
