package com.cannibalization.back.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ClientCardResponse(
        String clientId,
        UUID snapshotId,
        Instant asOfAt,
        Profile profile,
        Insurance insurance,
        Communications communications,
        Spending spending
) {

    public record Profile(
            Integer age,
            String gender,
            String region,
            String segment,
            Boolean payrollClient,
            Integer bankTenureMonths,
            Boolean familyOrChildren
    ) {
    }

    public record Insurance(
            Boolean activeDms,
            Boolean activeAccidentPolicy,
            Integer activePolicyCount,
            Boolean hadInsurancePolicy,
            Integer daysSinceDmsPurchase
    ) {
    }

    public record Communications(
            Integer oncoPushes30d,
            Integer allProductPushes7d,
            Integer allProductPushes30d,
            Integer daysSinceOncoPush,
            Integer daysSinceDmsPush,
            Double openRate180d,
            Double ctr180d,
            String previousPushProduct
    ) {
    }

    public record Spending(
            BigDecimal medicalSpend90d,
            BigDecimal pharmacySpend90d,
            BigDecimal avgMonthlySpend
    ) {
    }
}