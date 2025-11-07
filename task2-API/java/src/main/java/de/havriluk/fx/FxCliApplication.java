package de.havriluk.fx;

import de.havriluk.fx.persist.MongoWriter;
import de.havriluk.fx.persist.PostgresWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Scanner;

@SpringBootApplication
/**
 * This is the main application, built with the spring boot framework. 
 * It utiliyzes the FxApiClient class to connect to the api endpoint defined in 
 * src/main/resources/application.properties#api.base
 * Once the JSON Payload is retrieved, it prints it out to the SDOUT
 * and asks whether the user desires any persistence.
 * Should the user answer with "yes" then two subsequent prompts would ask 
 * him whether he want to persist to the Postgres backend(see src/main/java/de/havriluk/fx/persist/PostgresWriter)
 * and alternatively whether the persistence into the MondoDB store is desired (see src/main/java/de/havriluk/fx/persist/MongoWriter)
 * After persisting the application finishes its execution. 
 */
public class FxCliApplication implements CommandLineRunner {
    //api client
    private final FxApiClient apiClient;
    // PG DB writer
    private final PostgresWriter pgWriter;
    // Mongo DB writer
    private final MongoWriter mongoWriter;
    // Jackson Object Mapper
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Value("${api.baseCurrency:EUR}")
    private String baseCurrency;

    public FxCliApplication(FxApiClient apiClient, PostgresWriter pgWriter, MongoWriter mongoWriter) {
        this.apiClient = apiClient;
        this.pgWriter = pgWriter;
        this.mongoWriter = mongoWriter;
    }

    public static void main(String[] args) {
        SpringApplication.run(FxCliApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        //plain old java STD IN scanner reads and parses the options
        Scanner sc = new Scanner(System.in);
        // ask for the desired date
        LocalDate date = promptDate(sc);
        // query the API endpoint and retrieve JSON
        Map<String, Object> payload = apiClient.fetchRatesOnce(date, baseCurrency);
        //process the response payload
        Map<String, Double> rates = (Map<String, Double>) payload.get("rates");
        String base = (String) payload.get("base");
        String d = (String) payload.get("date");

        System.out.printf("Fetched %d rates for %s base %s%n",
                rates != null ? rates.size() : 0, d, base);
        System.out.println(mapper.writeValueAsString(rates));

        if (!promptYesNo(sc, "Persist data? (yes/no): ")) {
            System.out.println("Not persisted.");
            return;
        }

        if (pgWriter.isAvailable()) {
            if (promptYesNo(sc, "Persist into Postgres? (yes/no): ")) {
                int n = pgWriter.persistPayload(payload);
                System.out.printf("Persisted %d rows into Postgres.%n", n);
            }
        } else {
            System.out.println("Postgres is not available (skipping).");
        }

        if (mongoWriter.isAvailable()) {
            if (promptYesNo(sc, "Persist into MongoDB? (yes/no): ")) {
                int n = mongoWriter.persistPayload(payload);
                System.out.printf("Upserted %d document into MongoDB.%n", n);
            }
        } else {
            System.out.println("MongoDB is not available (skipping).");
        }
    }

    private LocalDate promptDate(Scanner sc) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        while (true) {
            System.out.print("Enter date (YYYY-MM-DD): ");
            String s = sc.nextLine().trim();
            try {
                return LocalDate.parse(s, fmt);
            } catch (Exception e) {
                System.out.println("Invalid format. Please use YYYY-MM-DD.");
            }
        }
    }

    private boolean promptYesNo(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim().toLowerCase();
            if ("y".equals(s) || "yes".equals(s) || "true".equals(s) || "1".equals(s)) return true;
            if ("n".equals(s) || "no".equals(s) || "false".equals(s) || "0".equals(s)) return false;
            System.out.println("Please answer yes or no.");
        }
    }
}