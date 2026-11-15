package com.cannibalization.back.client;

import java.time.Instant;
import java.util.UUID;

public record ClientCardResponse(
        String clientId,
        UUID snapshotId,
        Instant asOfAt
) {
}