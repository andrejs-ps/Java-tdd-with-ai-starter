# Java SE in Practice: TDD Fundamentals to AI-Assisted and Agentic Coding — demo project

A demo Java project built live in a video course on TDD with AI-assisted and agentic coding (audience: medior Java developers, new to TDD). The code is deliberately simple: a balance between trivial and real-world complex. This file and `.claude/` are local only (gitignored, never pushed).

## Working rules

- **Never start implementing until the user gives an explicit, final "go".** Discussing or agreeing on a design point is not a go.
- **Terminology:** always "Maven Module" (`exchange`, `broker`) or "Course Module" (course sections 1, 2, 3 ...). Never bare "module".
- **Course Module 2 lesson, built live on camera: "introduce EUR as a fallback for buying"** (pure unit tests). The starter trades through USD only: EUR balances can be opened and deposited into but are never used, and there is no currency conversion in the trading code. A pre-built `BuyingPowerValidator#canAfford` (boolean, USD only, with tests) refuses a buy whose cost plus the flat fee exceeds the USD balance; it is the base the lesson extends. Do NOT build the fallback (no EUR in the affordability check, no EUR debit in `applyFill`, no FX use in trading); how the lesson extends it is designed live. The FX rates code (`StaticDataProvider`, `CsvStaticDataProvider`, `fx-rates.csv`) is an already-built prerequisite, unwired on purpose.
- **Broker fee (decided):** a flat USD fee (`broker.fee-usd`, default 1.00) on every filled order. It is part of the buy affordability check (`orderCost + fee ≤ balance`); a sell has it taken from its proceeds, or no fee if the proceeds are smaller. Not in the cost basis, never charged in EUR. See `domain.md`.
- **Deliberately undecided (do not pick an answer, leave the FX fee out entirely):** is an FX conversion fee charged in the source or destination currency, and in what order relative to the conversion itself?
- Pre-built and not taught: `OrderValidator`, `BuyingPowerValidator` (USD only), `PortfolioUpdater#applyFill` (USD only), `OrderService`, account/deposit/balance endpoints, the `PortfolioStore` implementation, the `exchange` Maven Module.

## Where to look (plain paths, read on demand — not auto-imported)

- `.claude/docs/tech-stack.md` — stack, versions, what is out of scope
- `.claude/docs/domain.md` — domain concepts, order rules, `applyFill`
- `.claude/docs/flow.md` — end-to-end buy-order flow, across both Maven Modules and inside the broker
- `.claude/docs/endpoints.md` — exchange and broker endpoints with their status codes
- `.claude/docs/database.md` — what is stored in the H2 file, when, and how to wipe it
- `.claude/docs/existing-test-scenarios.md` — the tests that exist in the starter (unit, integration, end-to-end)
- `tdd-course-demo-project-brief.md` (repo root, untracked) — full design brief and idea graveyard

## Commands

- Run all tests: `mvn clean test` (repo root).
- Run the apps: `ExchangeMain` (port 7070), then `BrokerMain` (port 7080). Config in each Maven Module's `*.properties`; any key can be overridden with `-D`.
