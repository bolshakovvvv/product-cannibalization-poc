package com.cannibalization.back.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client_snapshot", schema = "cann_db")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientSnapshotEntity {

    @Id
    private UUID id;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "as_of_at", nullable = false)
    private Instant asOfAt;

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender")
    private String gender;

    @Column(name = "region")
    private String region;

    @Column(name = "segment")
    private String segment;

    @Column(name = "is_payroll_client")
    private Boolean payrollClient;

    @Column(name = "bank_tenure_months")
    private Integer bankTenureMonths;

    @Column(name = "has_family_or_children")
    private Boolean familyOrChildren;

    // Страховые продукты

    @Column(name = "has_active_dms")
    private Boolean activeDms;

    @Column(name = "has_active_accident_policy")
    private Boolean activeAccidentPolicy;

    @Column(name = "active_policy_count")
    private Integer activePolicyCount;

    @Column(name = "had_insurance_policy")
    private Boolean hadInsurancePolicy;

    @Column(name = "days_since_dms_purchase")
    private Integer daysSinceDmsPurchase;

    @Column(name = "onco_pushes_30d")
    private Integer oncoPushes30d;

    @Column(name = "all_product_pushes_7d")
    private Integer allProductPushes7d;

    @Column(name = "all_product_pushes_30d")
    private Integer allProductPushes30d;

    @Column(name = "days_since_onco_push")
    private Integer daysSinceOncoPush;

    @Column(name = "days_since_dms_push")
    private Integer daysSinceDmsPush;

    @Column(name = "open_rate_180d")
    private Double openRate180d;

    @Column(name = "ctr_180d")
    private Double ctr180d;

    @Column(name = "previous_push_product")
    private String previousPushProduct;

    @Column(name = "medical_spend_90d", precision = 18, scale = 2)
    private BigDecimal medicalSpend90d;

    @Column(name = "pharmacy_spend_90d", precision = 18, scale = 2)
    private BigDecimal pharmacySpend90d;

    @Column(name = "avg_monthly_spend", precision = 18, scale = 2)
    private BigDecimal avgMonthlySpend;
}