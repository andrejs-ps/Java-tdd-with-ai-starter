# Tech stack

- **Java 25**
- **Maven**, multi-module: Maven Modules `exchange`, `broker`.
- **Javalin 7** — a lightweight web framework and REST API for both broker and exchange modules
- **Jackson 3** for POJO<--->JSON conversion
- **`jackson-dataformat-csv`** (Jackson 3) — only for `fx-rates.csv`
- **`java.net.http.HttpClient`** — broker's outbound calls to the exchange
- **JUnit 6** — for both unit and integration tests;
- **RestAssured** — for integration tests
- **H2 (embedded)** — broker persistence through plain JDBC (`JdbcPortfolioStore`), plain SQL from `schema.sql`, no ORM.
- **Claude Code** — primary AI tool

## Intentional limitations: 
- no authentication or authorization (`accountId` is an unguarded UUID string)
- no rate limiting
- no concurrency handling (one request at a time per account is assumed)
