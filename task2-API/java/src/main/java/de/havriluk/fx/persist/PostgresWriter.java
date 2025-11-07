package de.havriluk.fx.persist;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; 

@Component
public class PostgresWriter {

    private final JdbcTemplate jdbc;

    public PostgresWriter(org.springframework.core.env.Environment env) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(env.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/rates"));
        ds.setUsername(env.getProperty("spring.datasource.username", "app"));
        ds.setPassword(env.getProperty("spring.datasource.password", "app"));
        this.jdbc = new JdbcTemplate(ds);
    }

    @PostConstruct
    public void ensureSchema() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS fx_rates (" +
                "rate_date date NOT NULL," +
                "base_currency text NOT NULL," +
                "currency text NOT NULL," +
                "rate numeric(18,8) NOT NULL," +
                "fetched_at timestamptz NOT NULL DEFAULT now()," +
                "source_etag text," +
                "payload_json jsonb," +
                "PRIMARY KEY (rate_date, base_currency, currency))");
        jdbc.execute("CREATE INDEX IF NOT EXISTS fx_rates_by_currency ON fx_rates(currency, rate_date)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS fx_rates_by_date ON fx_rates(rate_date)");
    }

    public boolean isAvailable() {
        try {
            Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1;
        } catch (Exception e) {
            return false;
        }
    }

    public int persistPayload(Map<String, Object> payload) {
        String rateDate = (String) payload.get("date");
        String base = (String) payload.get("base");
        Map<String, Object> rates = (Map<String, Object>) payload.get("rates");

        if (rates == null || rates.isEmpty()) return 0;

        String upsert = "INSERT INTO fx_rates (rate_date, base_currency, currency, rate, fetched_at, payload_json) " +
                "VALUES (?, ?, ?, ?, now(), to_jsonb(?::json)) " +
                "ON CONFLICT (rate_date, base_currency, currency) " +
                "DO UPDATE SET rate = EXCLUDED.rate, fetched_at = now(), payload_json = EXCLUDED.payload_json";

        List<Object[]> batch = rates.entrySet().stream()
            .map(e -> new Object[] {
              java.sql.Date.valueOf(rateDate),  // <-- cast string "YYYY-MM-DD" to SQL date
              base,
              e.getKey(),
             ((Number) e.getValue()).doubleValue(),
             JsonString(payload)               // JSON string for to_jsonb(?::json)
        }).collect(java.util.stream.Collectors.toList());

        int[] counts = jdbc.batchUpdate(upsert, batch);
        return counts.length;
    }

    private String JsonString(Object obj) {
        try {
            // tiny inline Jackson to avoid a separate ObjectMapper field
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }
}
