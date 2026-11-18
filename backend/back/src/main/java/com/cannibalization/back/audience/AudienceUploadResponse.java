package com.cannibalization.back.audience;

import java.util.UUID;

public record AudienceUploadResponse(
        UUID audienceId,
        String filename,
        int inputRowCount,
        int uniqueClientCount,
        int duplicateCount,
        int foundCount,
        int notFoundCount
) {
}