package de.havriluk.fx;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

@Component
public class FxApiClient {

    private final RestTemplate rest = new RestTemplate();

    @Value("${api.base:https://api.frankfurter.app}")
    private String apiBase;

    public Map<String, Object> fetchRatesOnce(LocalDate date, String baseCurrency) {
        String url = String.format("%s/%s?from=%s",
                apiBase.replaceAll("/$", ""),
                date.toString(),
                baseCurrency);
        ResponseEntity<Map> resp = rest.getForEntity(url, Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("Failed to fetch rates: " + resp.getStatusCode());
        }
        // Basic sanity checks
        Map body = resp.getBody();
        if (!body.containsKey("rates") || !body.containsKey("date") || !body.containsKey("base")) {
            throw new RuntimeException("Unexpected API payload structure");
        }
        return body;
    }
}