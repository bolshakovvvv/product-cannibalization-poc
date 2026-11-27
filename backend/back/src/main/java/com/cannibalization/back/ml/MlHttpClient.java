package com.cannibalization.back.ml;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class MlHttpClient {

    private final RestClient restClient;

    public MlHttpClient(@Value("${ml.base-url:http://localhost:8001}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public record Prediction(String clientId, Double probability) {}

    private record PredictRequest(List<MlFeatureService.ClientFeatures> clients) {}

    private record PredictResponse(String modelVersion, List<Prediction> predictions) {}

    public List<Prediction> predict(List<MlFeatureService.ClientFeatures> clients) {
        PredictResponse response = restClient.post()
                .uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PredictRequest(clients))
                .retrieve()
                .body(PredictResponse.class);

        if (response == null || response.predictions() == null) {
            throw new IllegalStateException("ML service returned no predictions");
        }
        return response.predictions();
    }
}
