# Java SE in Practice: TDD Fundamentals to AI-Assisted and Agentic Coding


**Purpose of this document:** this summarizes design decisions made in planning conversations for the demo Java project built live throughout the course. It's meant to give context if the demo project needs adjustment.

**Terminology:** always say "Maven Module" (`exchange`, `broker`) or "Course Module" (course section 1, 2, 3 ...). Never bare "module".

---

## 1. Context

- **Course:** "Java SE in Practice: TDD Fundamentals to AI-Assisted and Agentic Coding" — a video course.
- Teaches Test-Driven Development combined with AI-assisted and agentic coding practices, in Java.
- **Audience:** medior Java developers — comfortable with Java and JUnit, new to TDD.
- Draws on TDD / software-delivery thinking associated with Kent Beck
- **Central theme:** AI can assist at every stage of TDD (writing tests, writing code, refactoring, reviewing) under human direction — never unsupervised.

## 2. Terminal objectives

### 2a. Original

1. Apply the red, green, refactor cycle to produce well-designed, well-tested Java code in small, verifiable steps.
2. Write tests that express requirements and expected behavior clearly enough to serve as a specification for any implementer, human or AI.
3. Use an AI assistant within the TDD cycle by writing the failing test yourself, prompting for the implementation, and running the suite to verify it.
4. Evaluate AI-generated tests for missing edge cases, weak assertions, and tests that mirror the implementation rather than the requirement.
5. Refactor AI-produced code toward idiomatic, maintainable Java while using the test suite to confirm behavior is preserved.
6. Delegate a well-specified implementation task to an agentic tool using the test suite as the contract and acceptance gate.
7. Detect agent-completed work that passes the tests but violates the intent, including hardcoded answers, overfitting to assertions, and unspecified behavior changes.

### 2b. Rephrased (simplified)

Tag in brackets shows who does the work: **[Human]**, **[Both]** (human + AI), or a noted progression.

1. **[Human → later Human+AI]** Work in tiny Red-Green-Refactor steps: write one failing test, write just enough code to pass it, then clean up. Never skip a step.
2. **[Human]** Write tests precise enough that someone else — a teammate or an AI — could implement the feature correctly using only the test as their spec.
3. **[Both]** Write the failing test yourself, hand it to an AI to implement, then run the suite yourself to confirm it's actually green.
4. **[Human, AI-assisted optional]** Read AI-written tests like a skeptic: catch missing edge cases, weak assertions, and tests that restate the implementation instead of the requirement.
5. **[Both]** Refactor AI-generated code into clean, idiomatic Java — using the existing tests as your safety net.
6. **[Both]** Hand a well-specified task to an AI agent, with the test suite as the contract it must satisfy.
7. **[Human]** Spot when an agent "passed" by gaming you, not by actually solving the problem.
8. **[Human, AI-assisted optional]** Set up for the TDD process: record essential test techniques and test automation best practices into AI instructions. *(New objective, added after the original 7.)*

## 3. Tech stack

- **Java 25** (current LTS)
- **Maven**, multi-module: parent `pom` + Maven Modules `exchange`, `broker`.
- **Javalin 7** — HTTP layer for both Maven Modules (chosen over raw JDK `HttpServer` and over Spring Boot)
- **Jackson 3** — JSON both directions;
- **`jackson-dataformat-csv`** (Jackson 3) — only for `fx-rates.csv`
- **`java.net.http.HttpClient`** — broker's outbound calls to exchange (short request timeout)
- **JUnit 5 API on the JUnit 6 platform** — all tests
- **RestAssured** — all integration tests (real HTTP, raw JSON assertions)
- **H2 (embedded)** — broker persistence through plain JDBC (`JdbcPortfolioStore`), plain SQL from `schema.sql`, no ORM.
- **No Mockito, Spring, Sprint Boot, Docker**: keep things simple
- **No authentication or authorization anywhere:** `accountId` is an unguarded identifier (a server-generated UUID string). A real broker would require both and an ownership check per account.

## 4. Demo setup

Details live in `.claude/docs/` (plain paths, read on demand).

- **Business:** a personal investment-portfolio tracker for retail investors, one account each. They submit buy/sell limit orders, the broker fills them through a single exchange (NYSE), tracks holdings and exposes portfolio state as JSON. No UI. Concepts: `.claude/docs/domain.md`.
- **Currencies:** each account starts with a USD balance of 0 and may open one EUR balance. The starter trades through USD only; the EUR fallback is the Course Module 2 lesson.
- **Two Maven Modules:** `exchange` (external, simulated, flaky, stateless; not taught) and `broker` (what the course builds). Endpoint tables and examples: `.claude/docs/endpoints.md`.
- **Broker layers:** thin HTTP controllers, then services (`AccountService`, `OrderService`), a pure domain (unit-tested directly), and infrastructure adapters (`PortfolioStore`, `ExchangeClient`, `StaticDataProvider`). End-to-end flow: `.claude/docs/flow.md`.
- **Orders:** limit orders, fill or kill only; the exchange decides. Three outcomes: `FILLED`, `REJECTED` (the broker refused to send it), `KILLED` (the exchange did not fill it). A flat USD broker fee applies to every filled order (`broker.fee-usd`, default 1.00). Rules and fee: `.claude/docs/domain.md`.
- **Persistence:** embedded H2 through plain JDBC; FX rates from a static `fx-rates.csv` read once at startup. `.claude/docs/database.md`.
- **Pre-built and not taught** (each with its own tests): `OrderValidator`, `BuyingPowerValidator` (USD only), `applyFill` (USD only), FX rates loading, `OrderService` (tested with a fake `ExchangeClient`), the account / deposit / balance endpoints, `PortfolioStore`, and the `exchange` Maven Module.
- **Accepted limitations:** no authentication, no rate limiting, no concurrency handling, and an exchange timeout leaves the order outcome unknown (504, nothing saved). Stack: `.claude/docs/tech-stack.md`.
- **Open design point:** the FX conversion fee is deliberately undecided; see the Course Module 2 design below.

---

## Course Module 2 design: a new feature with TDD (EUR as a fallback for buying)

**Starting point:** the starter trades through USD only. EUR balances can be opened and deposited into but are never used, and no currency is converted in the trading code. A pre-built `BuyingPowerValidator#canAfford` (boolean, USD only, with tests) is the base the lesson extends; `applyFill` is USD only too. The FX rates code (`StaticDataProvider`, `CsvStaticDataProvider`, `fx-rates.csv`) is an already-built prerequisite, unwired in the starter on purpose. The lesson uses it to pull the rates, and it assumes the file is always there with correct data (happy path only; Course Module 3 makes this robust). Nothing of the fallback is pre-built; how the lesson extends the validator and `applyFill` is designed live.

**Testing:** 100% pure unit tests, no I/O. The tests inject the real `CsvStaticDataProvider`, built over an in-memory `StringReader` with a few rates (as the provider's own tests do), so there is no fake provider and no file access; accounts, balances and orders are hand-built records. The starter already pins today's limitation with a test ("a EUR balance never helps pay for a buy"), which is the first test to turn red.

### Flow (clips, in sequence)

1. **Clip 1: Where TDD does not help**
   - Not high-level architecture; yes program-level design (interfaces, method parameters).
   - TDD is a design technique through tests: Kent Beck's book spends half its length designing and redesigning the `Money` class. Tests are a positive by-product.
   - TDD doesn't exist in isolation: requirements → spec → tests → automated tests → impl → self review → code review. The course lives mostly in the tests-to-review loop (and it is a loop) and spends minimal time on requirements, maybe demoing "grill me" once. This repeats the Course Module 1 diagram on purpose, as a quick reminder.
2. **Clip 2: Project setup and introduction (about 7 minutes)**
   - Pull the code and give the project overview.
   - Why no Spring Boot (with a pointer to the TDD-with-Spring course); the project balances between toy and real-world.
   - Explain what is already there (slides).
3. **Clip 3: Strict RGR with AI** (terminal objective 1)
   - On a small existing feature: remove a few tests of `OrderValidator` or another service and redo them.
   - For more manual TDD, see the other course.
   - Strict RGR is meant for humans and is too restrictive with AI, so try test-first in small batches.
4. **Clip 4: Analyzing the new requirement** (terminal objective 2)
   - Introduce the EUR-as-fallback feature (the rule below).
   - Analyze it with `/plan` or an agent; mention Opus vs Sonnet (smarter vs average model).
   - The undecided FX-fee question surfaces here.
   - Produce the tests in English in a markdown file first, and the human reviews them. This is the critical point, so spend time on it: poor test understanding means shaky everything else.
5. **Clip 5: Strict TDD with AI** (terminal objective 3)
   - First complete the `/tdd` skill: follow RGR strictly, ONE failing test at a time.
   - Flow: `/tdd <task>` once per task, not on every prompt. Invoking it loads the skill text into the session, where it stays, so follow-ups like "next test" need no re-invoke.
   - The human writes 1-2 tests, the AI writes the implementation.
   - A good start and useful to get used to the process, but it feels too slow, so move to test-first.
6. **Clip 6: Implementing with the test-first approach** (terminal objective 5)
   - First loosen the `/tdd` skill: several failing tests at a time. The AI implements until all pass WITHOUT changing tests; otherwise it STOPs and alerts the human. Re-invoke `/tdd` after editing the skill, since the old text stays in context.
   - Tests become automated tests in batches.
   - Refactor whatever can be refactored.
7. **Clip 7: Viewing the generated code critically** (terminal objective 4)
   - Strict TDD done, then the looser test-first; now find problems, if any.
   - Talk about problems caught previously, off camera: see `test_problems.txt` at the root of the project.

### The USD-then-EUR fallback rule

This is the rule the Course Module 2 lesson introduces. The starter implements only step 1: it looks at USD alone and never touches EUR.

An account can hold a USD entry, a EUR entry, both, or neither. NYSE trades in USD, so `orderValue = limitPrice × quantity` is always a USD figure. The check:

1. Is the USD balance alone ≥ `orderValue` plus the flat USD fee? If yes, approve and deduct from USD only. EUR is never touched.
2. If not, convert the EUR balance to USD via `StaticDataProvider`'s rate. Is USD balance + converted EUR ≥ `orderValue` plus the fee? If yes, approve: draw down USD first, then convert only the remaining shortfall from EUR.
3. Still short even combined → reject. No exchange call either way (all of this is pre-computable).

**Worked example:** account holds $500 USD and €200 EUR, order costs $600. USD alone is short. Combined: $500 + (€200 ≈ $216 at 1 EUR = 1.08 USD) = $716 ≥ $601 (cost plus the $1 broker fee), so the order is affordable. All $500 USD is used, and only the $101 shortfall is converted from EUR (101 × 0.925926 = €93.5185), not the whole €200.

Selling stays USD-only: proceeds never auto-convert into EUR.

**Left for the lesson to decide:**
- The flat USD broker fee is part of the affordability check (cost + fee ≤ balance) and a USD-only charge in the starter; whether EUR may cover it is for the lesson to design.
- The FX conversion fee stays deliberately undecided (source or destination currency, and the order relative to the conversion; see the open question below). The exact EUR amount deducted for the $101 shortfall depends on it and on rounding order, which is why this example is left open rather than guessed.
- Affordability is judged on the order's limit price (the worst case), but the money actually taken is `fillPrice × quantity` plus the fee, and `fillPrice` is never worse than the limit for a buy.

### Open question: the FX conversion fee (do not decide it, flag it for the live demo)

Whether an FX conversion fee is charged in the source or destination currency, and in what order relative to the currency conversion itself. Deliberately undecided — meant to be discovered and resolved live, through a requirements-review exercise, as course content itself.

---

## Course Module 3 design: TDD at Integration level

**Starting point:** the end state of Course Module 2. The EUR fallback is built and unit-tested, and it uses the pre-built `CsvStaticDataProvider` to pull the FX rates, so the feature works over HTTP. Course Module 2 assumes the happy path only: `fx-rates.csv` is always there and holds correct data. Everything is green, yet nothing guards the real surfaces. The provider in particular has no protection: it reads the file in its constructor, and a missing file or bad data simply throws. Realizing that, and making the provider and the other integration interfaces robust, is the point of this Course Module.

### Flow (core ideas, in sequence)

1. TDD is not only for unit tests: the same write-test-first loop works for higher-level tests.
2. We focus on integration tests.
3. Identify the integration points of the setup: the client, the exchange, the local disk.
4. Ask the AI to analyze the three integration surfaces and produce the gaps (scenarios nobody thought of, and what the code does about each).
5. Produce a suite of tests from that analysis. Tests for behavior that already works pass on arrival (prove they can fail by breaking the code on purpose). Tests for missing behavior start red.
6. Review the suite and iterate on design decisions. Main one: a missing or invalid `fx-rates.csv` must not fail the broker; see [Decision to take live](#decision-to-take-live-a-bad-fx-file).
7. Terminal objective 6: delegate the red tests to an agent, with the suite as the contract and the acceptance gate.
8. Terminal objective 7 stays theoretical unless something relevant happens in the actual demo (models are non-deterministic): "we got none of these today, but this is what you might meet in the wild".

**Coverage lesson:** line and branch coverage can look excellent, yet never flag a scenario nobody thought of, because no code was written for it and so no line or branch is left uncovered. Examples below: tests 4, 10, 16.

### Integration surfaces

```
                         +---------------------------------+
 [1] CLIENT              |             BROKER              |
 (investor app)          |                                 |
  HTTP/JSON ------------>| Controllers -> Services -> Domain
  (6 endpoints)          |                  |      |       |
                         +------------------|------|-------+
                                            |      |
              [2] HttpExchangeClient -------+      +-------- [3] JdbcPortfolioStore
                  HTTP, short timeout                           JDBC, schema.sql
                         |                                          |
                         v                                          v
                 EXCHANGE :7070                              H2 file / in-memory

                 [4] CsvStaticDataProvider --> fx-rates.csv  (read once at startup)
```

### Where each kind of test lives

- **Exchange surface:** RestAssured against the real broker (in-memory H2, unique name per test) with the `StubExchange` over real HTTP, so the real `HttpExchangeClient` runs (both `GET /quotes` and `POST /orders`). The stub is a tiny Javalin server in the broker's test sources that returns an exact status / body / delay per test; a timeout is a stub delay longer than a short client timeout.
- **FX file surface:** `CsvStaticDataProvider` tested directly against a real file on disk (JUnit `@TempDir`; the constructor takes a `Reader`, so a `FileReader` does it). Reading a real file already makes it an integration test. A bigger one connects it to `BuyingPowerValidator`. No seam is added to `BrokerApp.create`: that entry point is not made to accept every dependency, and file handling is not tested from that high up.
- **Real exchange:** the real `exchange` Maven Module (`exchange.faults.enabled=false`) is used only for one or two end-to-end happy-path tests.
- **Client surface:** mostly covered by unit tests already, so an HTTP test adds only parsing and status mapping. One test per status code, not per scenario.

### Scenarios

Code: does production code handle it correctly today (Yes / Partly / Wrong / No). Tests: U = unit-tested, I = integration-tested, `-` = no test. State is as of the end of Course Module 2.

| # | Scenario | Expected | Code | Tests |
|---|---|---|---|---|
| **Exchange: connectivity (order and quotes)** | | | | |
| 1 | Exchange slower than the timeout | 504; for an order, nothing saved | Yes | U (order service propagation only) |
| 2 | Exchange answers non-200 (400, 500, 3xx) | 502; for an order, nothing saved | Yes | U (same) |
| 3 | Exchange is down (connection refused) | 502 | Yes | U (same) |
| 4 | 200 with an unusable body: malformed, missing price, unknown status or kill reason | Should be 502; today a 400 blaming the client, or an unhandled 500 | Wrong / No | - |
| **Exchange: quotes (positions)** | | | | |
| 5 | A held symbol is missing from the answer | 502 today (the code picked this, the spec never said) | Yes | - |
| 6 | Extra symbols in the answer | Ignored | Yes | - |
| 7 | Several positions | One batch call, symbols sorted | Yes | - |
| 8 | Empty portfolio | `[]`, zero exchange calls | Yes | I (partial: checks `[]`, not "no exchange call") |
| **Exchange: orders** | | | | |
| 9 | Exchange kills the order (`LIMIT_NOT_MET`, `UNKNOWN_SYMBOL`) | 200 `KILLED`, portfolio unchanged | Yes | U, plus I for `LIMIT_NOT_MET` only |
| 10 | Exchange fills, then the broker's save fails | No handling; money moved at the exchange, no local record | No | - |
| **Client (one test per status code is enough)** | | | | |
| 11 | Refused order (too little USD, or more than held) | 422 | Yes | U |
| 12 | Invalid side, quantity, limit price, symbol or time in force | 400 | Yes | U (validator only) |
| 13 | Unknown account or malformed accountId on orders and positions | 404 (a malformed UUID also gives 404) | Yes | U (orders service only) |
| **Disk and FX** | | | | |
| 14 | Data survives a restart on a file-based H2 | Same portfolio | Yes | I (in-memory round-trip only) |
| 15 | `fx-rates.csv` missing or unreadable (real file, `@TempDir`) | Fallback disabled, broker keeps working (see decision below) | Partly (throws, raw error) | - |
| 16 | `fx-rates.csv` malformed, header only, or a rate that is zero, negative or duplicated | Fallback disabled | Partly / No | - |
| 17 | Rate for a pair not in the file | Fallback disabled for that pair; today `IllegalArgumentException` | Yes | U |
| 18 | Real provider + `BuyingPowerValidator` on a real file | Valid file: the $600 buy is affordable. Broken file: EUR ignored, `INSUFFICIENT_FUNDS` | n/a | - |
| 19 | The fallback flag in the JSON | `true` with the bundled file (HTTP test, no seam needed); `false` with a disabled provider (service-level test) | n/a | - |
| **Happy path** | | | | |
| 0 | $500 USD + €200 EUR, a $600 buy through the API | 200 `FILLED`; all USD used, only the shortfall taken from EUR | n/a | - |

Tests 1-3, 5-9 and 14 cover existing protection and pass on arrival. Tests 4, 10, 15-19 are the red ones that carry new behavior, and are the pool the agent task is drawn from.

### Decision to take live: a bad FX file

**Today (end of Course Module 2):** `CsvStaticDataProvider` is wired in and fully used, but it is happy path only. A missing or malformed file crashes startup with a raw error, and a missing pair throws `IllegalArgumentException` mid-order (an unhandled 500). Nothing degrades gracefully, and nothing says "EUR fallback is off".

**Proposed behavior (to be reviewed and settled in the test-review step, not decided here):**
- One rule: the EUR fallback is available only if a valid EUR to USD rate was loaded. Tests 15, 16, 17 all fall under it.
- A bad file does not stop the broker. It starts with the fallback disabled, and USD-only buying keeps working.
- The broker exposes this to the client: a boolean field in the JSON, proposed on `GET /accounts/{accountId}`, `false` when the rates could not be loaded. The UI can then warn that EUR money can't be used until further notice. This is a design change recorded here only; the starter endpoints table is not edited.
- The cause is logged loudly at startup (a silent downgrade is the risk).
- Rates are read once at startup, so there is no recovery until the broker restarts; the UI message must not promise more.
- A buy refused while the fallback is off is a related open point: the client could see `INSUFFICIENT_FUNDS` while holding EUR. Decide whether the 422 carries the flag or gets its own reason.
- The Course Module 2 code needs a way to know the fallback is off (for example a provider that reports unavailable). That edits already unit-tested code, which the existing unit tests protect.

**Accepted gap:** no test starts `BrokerApp` with a broken file. The `false` flag in the JSON is covered at service level, not over HTTP.

### Terminal objective 6 demo

1. Write the scenario list in English in a markdown file; the human reviews it (same critical beat as Course Module 2 clip 4).
2. Produce the tests. Show the tests for existing behavior pass, break a line on purpose to prove they can fail, and show the red ones failing.
3. Settle the exact contract before delegating: flag name and location, status codes, the missing-symbol behavior. An open contract makes the agent invent details.
4. Delegate in a fresh session: make the red tests pass, change only `broker/src/main`, never edit tests or the stub, stop and report if a test looks wrong.
5. The human runs `mvn clean test` and checks that nothing under `src/test` changed (`git diff --stat`), then reviews the diff. This leads into objective 7.

### Terminal objective 7 (theory unless it shows up live)

What to look for in agent-completed work that passes the tests: hardcoded symbols or prices; a catch-all that satisfies the status assertions; a missing symbol silently valued at 0; a missing FX rate defaulted to 1.0 (EUR equals USD) or the exception swallowed with no logging; retries or a changed timeout nobody asked for; a portfolio saved on a timeout. Optional: keep a pre-made "gamed" diff, or examples from `test_problems.txt`.

### Self-practice scenario

**Refused orders, nothing saved**, both through the API: buying with too little USD, selling more than held, or selling a symbol not held is refused without contacting the exchange; invalid order details (side, quantity, limit price, symbol, time in force) are refused as invalid.

### Open questions (do not decide here)

- Test 4 reverses the earlier decision "an answer the exchange does give is assumed correct and well-formed" (see the idea graveyard). Include it, or keep that decision and drop test 4?
- Test 5: keep 502 as the code has it, or leave it open for discussion?
- The FX conversion fee question stays undecided and out of Course Module 3 as well.

---

## 5. Idea graveyard

Briefly, what was considered and rejected, and why.

### Stack / architecture

- **Spring Boot, Docker, JPA/Hibernate, Mockito** — too much setup weight and magic for a "vanilla Java" teaching pitch.
- **Concurrency handling, auth, rate limiting** — out of scope for the course;

### Exchange design

- **Exposed fault-injection query params on exchange (`?fault=timeout|malformed|...`)** — unrealistic; real exchanges don't expose a "break me" API. Replaced with an internal, unexposed random chance of slow responses.
- **Reserved "magic symbol" fault triggers (e.g. `SLOWCORP`) for deterministic fault testing** — dropped once it was confirmed all fault-path tests use the stub exchange, never the live flaky exchange.
- **Swapping the `ExchangeClient` for a fake at broker startup in integration tests** — replaced by the stub exchange over real HTTP, because "missing fields / different data structure" can only occur inside HTTP body parsing, which a typed fake would skip.
- **A separate `GET /quotes/{symbol}` endpoint alongside the batch endpoint** — cut as redundant; the batch endpoint handles n=1 fine, and dropping the single-symbol version sharpens the naive-loop-vs-batch-call teaching point.
- **Sanity-checking the exchange's fills in the broker (right symbol, full quantity, price within the limit)** — skipped on purpose; the broker assumes the exchange's answer is correct and only parses it.
- **Client order id for idempotent retries after a timeout** — not built. A timeout on `POST /orders` leaves the outcome unknown; the demo answers 504 and saves nothing, and accepts the gap. A possible later exercise.

### Broker domain design

- **Market orders** — replaced by limit orders (FOK only): a submitted price lets buying power be checked without calling the exchange. IOC, GTC / resting orders and FIFO cost basis are shelved as optional later exercises.
- **Order history (`OrderLedger`, `GET /orders`) and an `Order` state machine (NEW → SUBMITTED → FILLED)** — replaced by a stored, directly-mutated `Portfolio` and a final FILLED / REJECTED / KILLED outcome; no history or pending state was needed.
- **Affordability and stock checks inside `applyFill`** — replaced by invariants in the types plus the upstream validators.
- **200 with `REJECTED`, or 422, for orders the exchange killed** — rejections the broker makes itself are 422 (so clients can tell "business said no" from "request broken"); a kill is a normal outcome and passes through as 200 `KILLED`.
- **Broker fee blocking a sell, or counted in the cost basis** — the fee is taken from the sell proceeds (none if smaller) and stays out of the cost basis. A negative fee setting means no fee.
- **A live FX service** — kept as a static `fx-rates.csv`: no network call, no extra fault cases.



