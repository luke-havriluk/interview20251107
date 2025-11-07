package de.havriluk.fx.persist;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

@Component
public class MongoWriter {

    private final MongoClient client;
    private final String dbName;

    public MongoWriter(Environment env) {
        String uri = env.getProperty("spring.data.mongodb.uri",
                "mongodb://app:app@localhost:27017/rates");

        this.client = MongoClients.create(uri);

        // Determine DB name from URI (e.g. after the last '/')
        String dbParsed = null;
        String tail = uri.substring(uri.lastIndexOf('/') + 1);
        if (!tail.isEmpty()) {
            dbParsed = tail.contains("?") ? tail.substring(0, tail.indexOf('?')) : tail;
        }
        if (dbParsed == null || dbParsed.isEmpty()) {
            dbParsed = "rates";
        }

        this.dbName = dbParsed;

        // Ensure collection exists (no-op if already there)
        client.getDatabase(this.dbName).getCollection("fx_daily");
    }

    public boolean isAvailable() {
        try {
            client.getDatabase(this.dbName).runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int persistPayload(Map<String, Object> payload) {
        String rateDate = (String) payload.get("date");
        String base = (String) payload.get("base");
        Map<String, Object> rates = (Map<String, Object>) payload.get("rates");
        if (rates == null || rates.isEmpty()) return 0;

        String id = rateDate + "|" + base;

        MongoCollection<Document> coll = client.getDatabase(this.dbName).getCollection("fx_daily");
        Document doc = new Document("_id", id)
                .append("rate_date", rateDate)
                .append("base", base)
                .append("rates", rates)
                .append("fetched_at", Instant.now())
                .append("raw", payload);

        coll.replaceOne(eq("_id", id), doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
        return 1;
    }
}