package com.cannibalization.back.ml;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MlFeatureService {

    private final JdbcTemplate jdbc;

    public MlFeatureService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ClientFeatures(String clientId, Map<String, Object> features) {}

    private static final Map<String, String> COLUMNS = new LinkedHashMap<>();
    static {
        COLUMNS.put("Возраст", "age");
        COLUMNS.put("Пол", "gender");
        COLUMNS.put("Регион", "region");
        COLUMNS.put("Сегмент", "segment");
        COLUMNS.put("Зарплатный клиент", "is_payroll_client");
        COLUMNS.put("Стаж в банке, мес.", "bank_tenure_months");
        COLUMNS.put("Есть семья / дети", "has_family_or_children");
        COLUMNS.put("Онкострахование у связанных лиц", "related_person_has_onco");
        COLUMNS.put("Родитель 55+", "has_parent_over_55");

        COLUMNS.put("Медицинские траты за 90 дн., ₽", "medical_spend_90d");
        COLUMNS.put("Медицинские траты за 180 дн., ₽", "medical_spend_180d");
        COLUMNS.put("Аптеки за 90 дн., ₽", "pharmacy_spend_90d");
        COLUMNS.put("Аптеки за 90 дн., шт.", "pharmacy_transactions_90d");
        COLUMNS.put("Доля медицинских трат", "medical_spend_share");
        COLUMNS.put("Рост медицинских трат", "medical_spend_growth");
        COLUMNS.put("Средние траты в месяц, ₽", "avg_monthly_spend");
        COLUMNS.put("Нет медицинских трат в предыдущие 90 дн.", "no_medical_spend_previous_90d");
        COLUMNS.put("Нет трат за 180 дн.", "no_spend_180d");

        COLUMNS.put("Пушей по онко до T0, всего", "onco_pushes_total");
        COLUMNS.put("Пушей по онко за 30 дн.", "onco_pushes_30d");
        COLUMNS.put("Пушей по онко за 90 дн.", "onco_pushes_90d");
        COLUMNS.put("Дней с прошлого пуша по онко", "days_since_onco_push");
        COLUMNS.put("Первый пуш по онко", "is_first_onco_push");
        COLUMNS.put("Пушей по всем продуктам за 7 дн.", "all_product_pushes_7d");
        COLUMNS.put("Пушей по всем продуктам за 30 дн.", "all_product_pushes_30d");
        COLUMNS.put("Open rate за 180 дн.", "open_rate_180d");
        COLUMNS.put("CTR за 180 дн.", "ctr_180d");
        COLUMNS.put("Нет доставленных пушей за 180 дн.", "no_delivered_pushes_180d");
        COLUMNS.put("Открывал пуш по онко раньше", "opened_onco_push_before");
        COLUMNS.put("Покупки после прошлых пушей", "purchases_after_previous_pushes");
        COLUMNS.put("День недели отправки", "send_weekday");
        COLUMNS.put("Час отправки", "send_hour");

        COLUMNS.put("Был страховой полис раньше", "had_insurance_policy");
        COLUMNS.put("Активный ДМС на T0", "has_active_dms");
        COLUMNS.put("Дней с покупки ДМС", "days_since_dms_purchase");
        COLUMNS.put("ДМС куплен за 90 дн.", "purchased_dms_90d");
        COLUMNS.put("Активный НС на T0", "has_active_accident_policy");
        COLUMNS.put("Число активных полисов", "active_policy_count");
        COLUMNS.put("Пуш по ДМС за 7 дн.", "had_dms_push_7d");
        COLUMNS.put("Пуш по ДМС за 14 дн.", "had_dms_push_14d");
        COLUMNS.put("Пуш по ДМС за 30 дн.", "had_dms_push_30d");
        COLUMNS.put("Дней с последнего пуша по ДМС", "days_since_dms_push");
        COLUMNS.put("Открывал пуш по ДМС за 30 дн.", "opened_dms_push_30d");
        COLUMNS.put("Продукт предыдущего пуша", "previous_push_product");
        COLUMNS.put("Онко предложено раньше ДМС", "onco_offered_before_dms");
        COLUMNS.put("Онко предложено раньше НС", "onco_offered_before_accident");
    }

    public ClientFeatures forClient(String clientId) {
        List<ClientFeatures> result = jdbc.query("""
                SELECT * FROM cann_db.client_snapshot
                WHERE client_id = ?
                ORDER BY as_of_at DESC, id DESC
                LIMIT 1
                """, this::map, clientId);

        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        return result.get(0);
    }

    public List<ClientFeatures> forAudience(UUID audienceId) {
        return jdbc.query("""
                SELECT s.*
                FROM cann_db.audience_member m
                JOIN cann_db.client_snapshot s ON s.id = m.snapshot_id
                WHERE m.audience_id = ? AND m.status = 'FOUND'
                ORDER BY m.position
                """, this::map, audienceId);
    }

    private ClientFeatures map(ResultSet rs, int rowNumber) throws SQLException {
        Map<String, Object> features = new LinkedHashMap<>();
        for (var mapping : COLUMNS.entrySet()) {
            features.put(mapping.getKey(), rs.getObject(mapping.getValue()));
        }
        return new ClientFeatures(rs.getString("client_id"), features);
    }
}
