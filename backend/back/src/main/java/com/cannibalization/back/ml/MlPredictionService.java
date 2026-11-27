package com.cannibalization.back.ml;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One entry point for predictions: stable MOCK now, HTTP inference later.
 * Set ML_MODE=remote only when the Python inference service is ready.
 */
@Service
public class MlPredictionService {

    private final MlFeatureService features;
    private final MlHttpClient http;
    private final String mode;

    public MlPredictionService(MlFeatureService features,
                               MlHttpClient http,
                               @Value("${ml.mode:mock}") String mode) {
        if (!mode.equals("mock") && !mode.equals("remote")) {
            throw new IllegalArgumentException("ml.mode must be mock or remote");
        }
        this.features = features;
        this.http = http;
        this.mode = mode;
    }

    public String mode() {
        return mode.equals("remote") ? "ML" : "MOCK";
    }

    public double predictClient(String clientId) {
        var client = features.forClient(clientId); // Also checks that client exists.
        return predict(List.of(client)).get(clientId);
    }

    public Map<String, Double> predictAudience(UUID audienceId) {
        return predict(features.forAudience(audienceId));
    }

    private Map<String, Double> predict(List<MlFeatureService.ClientFeatures> clients) {
        Map<String, Double> result = new LinkedHashMap<>();
        if (clients.isEmpty()) return result;

        if (mode.equals("mock")) {
            for (var client : clients) {
                double probability = (10 + Math.floorMod(client.clientId().hashCode(), 81)) / 100.0;
                result.put(client.clientId(), probability);
            }
            return result;
        }

        // Small batches avoid sending a full audience in one huge HTTP request.
        Map<String, Double> received = new HashMap<>();
        for (int offset = 0; offset < clients.size(); offset += 200) {
            var batch = clients.subList(offset, Math.min(offset + 200, clients.size()));
            for (var prediction : http.predict(batch)) {
                if (prediction.clientId() == null || prediction.probability() == null
                        || !Double.isFinite(prediction.probability())
                        || prediction.probability() < 0 || prediction.probability() > 1) {
                    throw new IllegalStateException("Invalid ML prediction");
                }
                if (received.putIfAbsent(prediction.clientId(), prediction.probability()) != null) {
                    throw new IllegalStateException("ML returned duplicate client ID: " + prediction.clientId());
                }
            }
        }
        for (var client : clients) {
            Double probability = received.get(client.clientId());
            if (probability == null) {
                throw new IllegalStateException("ML returned no prediction for: " + client.clientId());
            }
            result.put(client.clientId(), probability);
        }
        if (result.size() != received.size()) {
            throw new IllegalStateException("ML returned unexpected client IDs");
        }
        return result;
    }
}
