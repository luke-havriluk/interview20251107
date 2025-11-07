# FX CLI (Spring Boot, Java 14)

## Prereqs
- JDK 14
- Maven 3.8+
- Your Docker compose with Postgres + Mongo running

## Configure
Edit `src/main/resources/application.properties` if needed.
Defaults assume:
- Postgres: jdbc:postgresql://localhost:5432/rates (app/app)
- Mongo:    mongodb://app:app@localhost:27017/rates?authSource=admin

## Run
mvn -q -DskipTests spring-boot:run

# Then follow prompts:
Enter date (YYYY-MM-DD): 2025-11-06
... (pretty-printed rates) ...
Persist data? (yes/no): yes
Persist into Postgres? (yes/no): yes
Persisted 30 rows into Postgres.
Persist into MongoDB? (yes/no): yes
Upserted 1 document into MongoDB.

## Build fat jar
mvn -q -DskipTests clean package
java -jar target/fx-cli-0.0.1.jar