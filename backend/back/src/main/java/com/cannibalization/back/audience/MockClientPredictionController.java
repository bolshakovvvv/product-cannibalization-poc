package com.cannibalization.back.audience;

import com.cannibalization.back.ml.MlPredictionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/clients")
public class MockClientPredictionController {

    private final MlPredictionService ml;

    public MockClientPredictionController(MlPredictionService ml) {
        this.ml = ml;
    }

    @PostMapping("/{clientId}/predict")
    public Map<String, Object> predict(@PathVariable String clientId) {
        double probability = ml.predictClient(clientId);
        String segment = ml.mode().equals("MOCK")
                ? (probability >= 0.7 ? "HIGH" : probability >= 0.4 ? "MEDIUM" : "LOW")
                : "UNRANKED";

        return Map.of(
                "clientId", clientId,
                "mode", ml.mode(),
                "probability", probability,
                "segment", segment
        );
    }
}
