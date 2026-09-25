package com.pulsewallet.pulsewallet.controller;

import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;

/**
 * Liveness endpoint used to confirm a fresh clone is wired up correctly.
 *
 * <p>{@code GET /api/health} reports the active profiles and whether the
 * database connection actually works. The profile echo is deliberate: the most
 * common Milestone 1 confusion is running with an unexpected profile and not
 * understanding why configuration looks wrong.
 *
 * <p>Spring Boot Actuator's {@code /actuator/health} covers similar ground in
 * more depth. This endpoint stays because it needs no authentication once
 * Milestone 2 locks the API down, which keeps it usable as a smoke test.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;
    private final Environment environment;
    private final String applicationName;

    public HealthController(
            DataSource dataSource,
            Environment environment,
            @Value("${spring.application.name}") String applicationName) {
        this.dataSource = dataSource;
        this.environment = environment;
        this.applicationName = applicationName;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("application", applicationName);
        details.put("status", "UP");
        details.put("activeProfiles", environment.getActiveProfiles());
        details.put("database", databaseStatus());
        details.put("checkedAt", Instant.now());
        return ApiResponse.ok("PulseWallet is running", details);
    }

    /**
     * Borrows a pooled connection and validates it. Reports DOWN instead of
     * throwing, so the endpoint still answers when Postgres is unreachable -
     * an endpoint that 500s tells you far less than one that says "app UP,
     * database DOWN".
     */
    private String databaseStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception ex) {
            return "DOWN";
        }
    }
}
