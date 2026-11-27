
package com.cannibalization.back.audience;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import com.cannibalization.back.ml.MlPredictionService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AudienceService {

    private final NamedParameterJdbcTemplate jdbc;
    private final MlPredictionService ml;

    public AudienceService(DataSource dataSource, MlPredictionService ml) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.ml = ml;
    }

    @Transactional
    public AudienceUploadResponse upload(MultipartFile file) {
        Set<String> clientIds = new LinkedHashSet<>();
        int rowCount = 0;

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8)
                    .replace("\uFEFF", "");

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .get();

            try (CSVParser parser = CSVParser.parse(content, format)) {
                for (var row : parser) {
                    String clientId = row.get("client_id");

                    if (!clientId.isBlank()) {
                        clientIds.add(clientId);
                        rowCount++;
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (clientIds.isEmpty()) {
            throw new IllegalArgumentException("CSV не содержит клиентов");
        }

        Map<String, UUID> snapshots = new HashMap<>();

        jdbc.query("""
                SELECT DISTINCT ON (client_id) client_id, id
                FROM cann_db.client_snapshot
                WHERE client_id IN (:ids)
                ORDER BY client_id, as_of_at DESC, id DESC
                """,
                Map.of("ids", clientIds),
                rs -> {
                    snapshots.put(
                            rs.getString("client_id"),
                            rs.getObject("id", UUID.class)
                    );
                });

        UUID audienceId = UUID.randomUUID();
        int duplicates = rowCount - clientIds.size();

        jdbc.update("""
                INSERT INTO cann_db.audience
                    (id, source_filename, input_row_count, duplicate_count)
                VALUES (:id, :filename, :rows, :duplicates)
                """,
                new MapSqlParameterSource()
                        .addValue("id", audienceId)
                        .addValue("filename", file.getOriginalFilename())
                        .addValue("rows", rowCount)
                        .addValue("duplicates", duplicates));

        List<SqlParameterSource> members = new ArrayList<>();
        int position = 1;
        int foundCount = 0;

        for (String clientId : clientIds) {
            UUID snapshotId = snapshots.get(clientId);

            if (snapshotId != null) {
                foundCount++;
            }

            members.add(new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID())
                    .addValue("audienceId", audienceId)
                    .addValue("clientId", clientId)
                    .addValue("position", position++)
                    .addValue("snapshotId", snapshotId)
                    .addValue("status",
                            snapshotId != null ? "FOUND" : "NOT_FOUND"));
        }

        jdbc.batchUpdate("""
                INSERT INTO cann_db.audience_member
                    (id, audience_id, client_id, position, snapshot_id, status)
                VALUES
                    (:id, :audienceId, :clientId, :position, :snapshotId, :status)
                """,
                members.toArray(new SqlParameterSource[0]));

        return new AudienceUploadResponse(
                audienceId,
                file.getOriginalFilename(),
                rowCount,
                clientIds.size(),
                duplicates,
                foundCount,
                clientIds.size() - foundCount
        );
    }

    public List<Map<String, Object>> getMembers(UUID audienceId) {
        return jdbc.queryForList("""
            SELECT
                client_id AS "clientId",
                status,
                snapshot_id AS "snapshotId"
            FROM cann_db.audience_member
            WHERE audience_id = :id
            ORDER BY position
            """, Map.of("id", audienceId));
    }


    public Map<String, Object> analyze(UUID audienceId) {
        var members = getMembers(audienceId);
        var probabilities = ml.predictAudience(audienceId);
        var ranking = AudienceRanking.rank(probabilities);
        var predictions = new ArrayList<Map<String, Object>>();

        boolean realMl = ml.mode().equals("ML");
        int high = 0, medium = 0, low = 0;
        double sum = 0;
        double selectedSum = 0;
        int analyzed = 0;
        int top10Count = 0;

        for (var member : members) {
            var result = new LinkedHashMap<>(member);
            String clientId = (String) member.get("clientId");
            Double probability = probabilities.get(clientId);

            if (probability != null) {
                var place = ranking.get(clientId);
                analyzed++;
                sum += probability;
                result.put("probability", probability);
                result.put("rank", place.rank());
                result.put("isTop10", place.top10());
                if (place.top10()) {
                    top10Count++;
                    selectedSum += probability;
                }

                if (realMl) {
                    // Relative priority is based on ranking, not fixed score bands.
                    result.put("segment", place.top10() ? "TOP_10" : "OTHER");
                } else {
                    // Preserve the earlier MOCK demonstration for backward compatibility.
                    String segment = probability >= 0.7 ? "HIGH"
                            : probability >= 0.4 ? "MEDIUM" : "LOW";
                    switch (segment) {
                        case "HIGH" -> high++;
                        case "MEDIUM" -> medium++;
                        case "LOW" -> low++;
                    }
                    result.put("segment", segment);
                }
            } else {
                result.put("probability", null);
                result.put("rank", null);
                result.put("isTop10", false);
                result.put("segment", "NO_DATA");
            }
            predictions.add(result);
        }

        // Ranked clients first, missing clients last. Alphabetical tie-break for missing IDs.
        predictions.sort(Comparator
                .comparingInt((Map<String, Object> item) ->
                        item.get("rank") == null ? Integer.MAX_VALUE : ((Number) item.get("rank")).intValue())
                .thenComparing(item -> (String) item.get("clientId")));

        var summary = new LinkedHashMap<String, Object>();
        summary.put("totalClients", members.size());
        summary.put("analyzedClients", analyzed);
        summary.put("averageProbability",
                analyzed == 0 ? null : Math.round(sum / analyzed * 10000.0) / 10000.0);
        // Counts refer ONLY to members with a calculated prediction.
        summary.put("top10Count", top10Count);
        summary.put("otherCount", analyzed - top10Count);
        summary.put("top10AverageProbability",
                top10Count == 0 ? null : Math.round(selectedSum / top10Count * 10000.0) / 10000.0);
        // Old counters are for MOCK, not probability bands in real ML.
        summary.put("highCount", high);
        summary.put("mediumCount", medium);
        summary.put("lowCount", low);

        return Map.of(
                "audienceId", audienceId,
                "mode", ml.mode(),
                "summary", summary,
                "predictions", predictions
        );
    }
}
