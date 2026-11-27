package com.cannibalization.back.audience;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ranks clients within ONE audience. This is a marketing priority ranking,
 * not a model for estimating causal cannibalization or uplift.
 */
public final class AudienceRanking {
    private AudienceRanking() {}

    public record Place(int rank, boolean top10) {}

    public static Map<String, Place> rank(Map<String, Double> probabilities) {
        var sorted = new ArrayList<>(probabilities.entrySet());
        for (var prediction : sorted) {
            Double p = prediction.getValue();
            if (p == null || !Double.isFinite(p) || p < 0 || p > 1) {
                throw new IllegalArgumentException("Invalid probability for: " + prediction.getKey());
            }
        }

        // Reproducible order for identical scores: client ID breaks ties.
        sorted.sort(Comparator
                .<Map.Entry<String, Double>>comparingDouble(Map.Entry::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey));

        // For n=100 -> 10 selected; for n=101 -> 11. For n=1 -> 1.
        int selected = (sorted.size() + 9) / 10;
        Map<String, Place> result = new LinkedHashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            result.put(sorted.get(i).getKey(), new Place(i + 1, i < selected));
        }
        return result;
    }
}
