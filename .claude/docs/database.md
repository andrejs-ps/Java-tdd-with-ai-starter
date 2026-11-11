# Database

The broker stores its state in an embedded H2 database, in the file `data/broker.mv.db`. Tests use a unique in-memory database each, which disappears when the test JVM ends.


## Working Life cycle

1. **You start the app**
   - The database starts with the app. H2 runs inside the broker's process.
   - If `data/broker.mv.db` does not exist, H2 creates it. Then `schema.sql` runs on every start. It creates tables the first time and does nothing on later starts.
   

2. **You use the endpoints**
   - `POST /accounts` stores the account and a USD balance of 0.
   - `POST /balances` stores a EUR balance of 0.
   - `POST /deposits` updates the balance amount.
   - A filled order updates the cash and the position together.
   - Killed orders, refused orders and exchange timeouts store nothing.


3. **You shut down the app**
   - Nothing is erased. Accounts, balances and positions are still in the file and are there on the next start.
   - Orders are not stored.


4. **You want to wipe the state clean**
   - Stop the broker.
   - Delete `data/broker.mv.db`, or the whole `data/` folder.
   - Start the broker again. It creates a fresh, empty database.
   - The same wipe is needed after any change to `schema.sql`, because `IF NOT EXISTS` never alters tables that already exist.


## Tables and persistence

- Plain SQL, no ORM and no migration tool. Tables: `accounts`, `cash_balances`, `positions`. Saving a `Portfolio` is one transaction.
- `PortfolioStore` has one implementation (plain JDBC) with its own test. The repository layer is not unit-tested or demoed in the course; it is exercised through the Course Module 3 integration tests.


## Static data: `fx-rates.csv`

- Static reference data for currency conversion, read by `StaticDataProvider`. This file, not a service, is the "exchange rate source".
- An already-built prerequisite for the Course Module 2 lesson: the code and its tests exist, but nothing in the broker uses it until the lesson wires it in.
- Read once at process startup (the broker is assumed to restart every day outside trading hours), so FX data is always in memory. Good enough for teaching; pulling fresh rates every hour or so would be a real-world next feature (out of course scope).
