package com.cannibalization.back.audience;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/clients")
public class MockClientPredictionController {

    private final JdbcTemplate jdbc;

    public MockClientPredictionController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/{clientId}/predict")
    public Map<String, Object> predict(@PathVariable String clientId) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM cann_db.client_snapshot WHERE client_id = ?)",
                Boolean.class,
                clientId
        );
        if (!Boolean.TRUE.equals(exists)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }

        // Та же формула MOCK, что и в массовом AudienceService.analyze().
        double probability = (10 + Math.floorMod(clientId.hashCode(), 81)) / 100.0;
        String segment = probability >= 0.7 ? "HIGH"
                : probability >= 0.4 ? "MEDIUM" : "LOW";

        return Map.of(
                "clientId", clientId,
                "mode", "MOCK",
                "probability", probability,
                "segment", segment
        );
    }
}
