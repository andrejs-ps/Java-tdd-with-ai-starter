# Endpoints

## Overview

### Exchange (2)

Simulated and stateless; prices are plain JSON numbers, always USD.

| Endpoint | Purpose | Responses |
|---|---|---|
| `GET /quotes?symbols=A,B,C` | current price per listed symbol (one or many); unlisted symbols are left out. Prices move randomly by up to `exchange.price-fluctuation`. | 200 `{SYMBOL: {price, timestamp}}` (`{}` if none is listed); 400 `symbols` missing |
| `POST /orders` | body: `clientOrderId`, `symbol`, `side`, `quantity`, `limitPrice`, `timeInForce` (FOK only). Fill or kill, in full at the listed price: a BUY fills if price ≤ limit, a SELL if price ≥ limit; an unlisted symbol is killed. | 200 filled `{orderId, clientOrderId, status: FILLED, symbol, side, quantity, price, timestamp}` or killed `{orderId, clientOrderId, status: KILLED, symbol, side, quantity, reason: LIMIT_NOT_MET \| UNKNOWN_SYMBOL, timestamp}`; 400 malformed order |

- Both endpoints have a random chance of a slow response (long enough to trip the broker client's timeout), switched off with `exchange.faults.enabled=false`.
- A killed order is a normal answer, not an error. `orderId` is the exchange's own reference; the broker does not use it yet.

### Broker (6)

Everything is scoped under `/accounts/{accountId}`; `accountId` is a UUID string.

| Endpoint | Purpose | Responses |
|---|---|---|
| `POST /accounts` | body: `investorName`. Creates investor + account with USD balance 0. | 201; 400 on invalid |
| `GET /accounts/{accountId}` | the cash balances | 200 / 404 |
| `POST /accounts/{accountId}/balances` | body: `currency`. Opens a zero balance; EUR only. | 201 / 404 / 409 (already open) / 400 (other currency) |
| `POST /accounts/{accountId}/deposits` | body: `currency`, `amount`. Deposits into an already open balance. | 200 / 404 (account or balance missing) / 400 (amount ≤ 0) |
| `POST /accounts/{accountId}/orders` | body: `symbol`, `side`, `quantity`, `limitPrice`, `timeInForce` (optional, FOK). | 200 when the order reached the exchange: filled `{status: FILLED, symbol, side, quantity, fillPrice, filledAt}` or killed `{status: KILLED, symbol, side, quantity, reason: LIMIT_NOT_MET \| UNKNOWN_SYMBOL}`; 422 `{status: REJECTED, reason: INSUFFICIENT_FUNDS \| INSUFFICIENT_POSITION}` when the broker refuses to send it; 400 invalid input; 404 unknown account; 504 exchange timeout; 502 exchange error |
| `GET /accounts/{accountId}/positions` | current holdings per symbol: quantity, cost basis, live market value, unrealized gain/loss. Computed at read time by combining stored `Position`s with ONE batch exchange quote call (empty portfolio → no exchange call). | 200 / 404 / 502 / 504 |

- Orders: 200 means the order reached the exchange and got an answer (filled or killed; a kill is a normal outcome for fill or kill). 422 means the broker refused to send it, so no exchange call is made. The portfolio is saved only when the order was filled.
- Earlier designs had a market-order-only flow and a `GET /orders` history endpoint. Both were superseded (see the idea graveyard in `tdd-course-demo-project-brief.md`).

## Exchange

Simulated and stateless. Each symbol has a base price, and quotes and fills move randomly around it by up to `exchange.price-fluctuation` (2% in `exchange.properties`; 0 means fixed prices). A random chance of a slow response (both endpoints) can be switched off with `exchange.faults.enabled=false`.

1. `GET /quotes?symbols=A,B,C`

   Gives the current price of one or more stocks. Stocks the exchange doesn't list are left out of the answer.

   Request: `GET /quotes?symbols=AAPL,MSFT`

   Response (200):
   ```json
   {
     "AAPL": {"price": 231.42, "timestamp": "2026-10-01T09:30:00Z"},
     "MSFT": {"price": 415.10, "timestamp": "2026-10-01T09:30:00Z"}
   }
   ```

   All symbols unknown gives `{}`. Errors: 400 if `symbols` is missing.

2. `POST /orders`

   Asks the exchange to buy or sell, fill or kill: the whole quantity is traded at the exchange's price right now, or the order is killed and nothing happens. A killed order is a normal answer, not an error. `clientOrderId` is the sender's own reference and is echoed back; `orderId` is the exchange's own reference.

   Request:
   ```json
   {
     "clientOrderId": "c-1",
     "symbol": "AAPL",
     "side": "BUY",
     "quantity": 10,
     "limitPrice": 250.00,
     "timeInForce": "FOK"
   }
   ```

   Response (200), filled:
   ```json
   {
     "orderId": "b6c1f0e2-5a7d-4c1e-9f3a-2d8e4a7b1c90",
     "clientOrderId": "c-1",
     "status": "FILLED",
     "symbol": "AAPL",
     "side": "BUY",
     "quantity": 10,
     "price": 231.42,
     "timestamp": "2026-10-01T09:30:00Z"
   }
   ```

   Response (200), killed: same shape with `"status": "KILLED"` and `"reason": "LIMIT_NOT_MET"` or `"UNKNOWN_SYMBOL"` instead of `price`.

   Errors: 400 if the order is malformed (including a missing `clientOrderId`).

## Broker

All account endpoints are under `/accounts`; `accountId` is a UUID string. Malformed or unknown id → 404.

1. `POST /accounts`

   Opens a new account for an investor. Every account starts with an empty USD balance.

   Request:
   ```json
   {"investorName": "Ada"}
   ```

   Response (201):
   ```json
   {
     "accountId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
     "investorName": "Ada",
     "balances": [{"currency": "USD", "amount": 0}]
   }
   ```

   Errors: 400 if the name is blank or the JSON is malformed.


2. `GET /accounts/{id}`

   Shows the investor's name and the cash in each balance.

   Request: `GET /accounts/{id}`

   Response (200):
   ```json
   {
     "accountId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
     "investorName": "Ada",
     "balances": [{"currency": "USD", "amount": 2684.80}]
   }
   ```

   Errors: 404 unknown account.

3. `POST /accounts/{id}/balances`

   Opens a EUR balance next to the USD one. Only EUR can be opened, and only once.

   Request:
   ```json
   {"currency": "EUR"}
   ```

   Response (201): the account, now with two balances (USD and EUR, both 0).

   Errors: 409 if the balance is already open (USD always is), 400 for any other currency, 404 for an unknown account.

4. `POST /accounts/{id}/deposits`

   Adds money to an open balance.

   Request:
   ```json
   {"currency": "USD", "amount": 5000}
   ```

   Response (200): the account with its updated balances.

   Errors: 400 if the amount is zero or negative, 404 if the account is unknown or that balance isn't open.

5. `POST /accounts/{id}/orders`

   Places a buy or sell order at a limit price: the most you will pay, or the least you will accept. The broker first refuses orders the account can't back (not enough USD to cover a buy plus the broker's flat fee, or not enough shares to sell) without contacting the exchange. Otherwise the exchange fills the whole order at its price or kills it. Only a filled order changes the account, and only a filled order is charged the fee: a buy pays it on top of the price, a sell has it taken from the proceeds (no fee if the proceeds are smaller than it). `timeInForce` is optional and can only be `FOK`.

   Request:
   ```json
   {"symbol": "AAPL", "side": "BUY", "quantity": 10, "limitPrice": 250.00}
   ```

   Response (200), filled:
   ```json
   {
     "orderId": "3f9a7c2d-8b14-4e6a-a1d5-6c0e9b2f7d44",
     "status": "FILLED",
     "symbol": "AAPL",
     "side": "BUY",
     "quantity": 10,
     "fillPrice": 231.42,
     "filledAt": "2026-10-01T09:30:00Z"
   }
   ```

   Response (200), killed. A normal outcome for fill or kill; nothing changes. Here the limit of 200 was below the exchange's price of 231.42:
   ```json
   {
     "orderId": "9d2b5e41-6f0a-4b7c-8e13-5a4c7d9f2b68",
     "status": "KILLED",
     "symbol": "AAPL",
     "side": "BUY",
     "quantity": 10,
     "reason": "LIMIT_NOT_MET"
   }
   ```
   The other reason is `UNKNOWN_SYMBOL`, when the exchange doesn't list the stock.

   Response (422), refused by the broker: `{"orderId": "...", "status": "REJECTED", "reason": "INSUFFICIENT_FUNDS"}` or `"INSUFFICIENT_POSITION"`.

   Errors: 400 invalid input, 404 unknown account, 504 the exchange didn't answer in time, 502 the exchange returned an error.

6. `GET /accounts/{id}/positions`

   Shows what the account holds: for each stock the quantity, what it cost, what it is worth now, and the gain or loss. Current values come from one batch call to the exchange (none for an empty portfolio). All amounts are USD.

   Request: `GET /accounts/{id}/positions`

   Response (200):
   ```json
   {
     "positions": [
       {
         "symbol": "AAPL",
         "quantity": 10,
         "costBasis": 2314.20,
         "marketValue": 2400.00,
         "unrealizedGainLoss": 85.80
       }
     ]
   }
   ```

   Errors: 404 unknown account, 504 or 502 if the exchange can't give prices.
