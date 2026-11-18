
package com.cannibalization.back.audience;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
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

    public AudienceService(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
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
        var predictions = new ArrayList<Map<String, Object>>();

        int high = 0;
        int medium = 0;
        int low = 0;
        double sum = 0;

        for (var member : members) {
            var result = new LinkedHashMap<>(member);

            if ("FOUND".equals(member.get("status"))) {
                String clientId = (String) member.get("clientId");

                double probability =
                        (10 + Math.floorMod(clientId.hashCode(), 81)) / 100.0;

                String segment = probability >= 0.7 ? "HIGH"
                        : probability >= 0.4 ? "MEDIUM" : "LOW";

                result.put("probability", probability);
                result.put("segment", segment);

                sum += probability;

                switch (segment) {
                    case "HIGH" -> high++;
                    case "MEDIUM" -> medium++;
                    case "LOW" -> low++;
                }
            } else {
                result.put("probability", null);
                result.put("segment", "NO_DATA");
            }

            predictions.add(result);
        }

        int analyzed = high + medium + low;

        var summary = new LinkedHashMap<String, Object>();

        summary.put("totalClients", members.size());
        summary.put("analyzedClients", analyzed);
        summary.put("averageProbability",
                analyzed == 0 ? null
                        : Math.round(sum / analyzed * 100.0) / 100.0);
        summary.put("highCount", high);
        summary.put("mediumCount", medium);
        summary.put("lowCount", low);

        return Map.of(
                "audienceId", audienceId,
                "mode", "MOCK",
                "summary", summary,
                "predictions", predictions
        );
    }

}
