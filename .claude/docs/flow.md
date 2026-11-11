# Flow

## End-to-end example: fund, buy, see the position

```
Investor                Broker                                      Exchange
   |                        |                                             |
   | POST /accounts         |                                             |
   |----------------------->| create account, USD balance 0               |
   |<-----------------------| accountId                                   |
   |                        |                                             |
   | POST /deposits USD 5000|                                             |
   |----------------------->| USD balance = 5000                          |
   |<-----------------------| ok                                          |
   |                        |                                             |
   | POST /orders           |                                             |
   | BUY 10 AAPL @ 250      |                                             |
   |----------------------->| 1. load portfolio                           |
   |                        | 2. order rules ok?       (else REJECTED)    |
   |                        | 3. USD covers 2500 + 1 fee? (else REJECTED) |
   |                        |                                             |
   |                        | POST /orders (fill or kill)                 |
   |                        |-------------------------------------------->|
   |                        |                           price 240 <= 250  |
   |                        |<--------------------------------------------| FILLED @ 240
   |                        | 4. debit 2400 + 1 fee from USD              |
   |                        | 5. add 10 AAPL, cost basis 2400             |
   |                        | 6. save portfolio                           |
   |<-----------------------| FILLED                                      |
   |                        |                                             |
   |                        |  (if price > limit: exchange answers KILLED |
   |                        |   and nothing is saved)                     |
   |                        |  (if no answer in time: nothing is saved)   |
   |                        |                                             |
   | GET /positions         |                                             |
   |----------------------->| load positions                              |
   |                        | GET /quotes?symbols=AAPL (one batch call)   |
   |                        |-------------------------------------------->|
   |                        |<--------------------------------------------| AAPL 240
   |                        | value = 10 x 240, gain/loss = 0             |
   |<-----------------------| qty 10, cost 2400, value 2400               |
```

## Inside the broker: a successful buy order

```
Client
  | POST /accounts/{id}/orders  {"symbol":"AAPL","side":"BUY","quantity":10,"limitPrice":250}
  v
OrderController (http)
  | parse id + body                       → OrderRequest (checks quantity > 0, limit > 0, USD limit; else 400)
  v
OrderService#placeOrder (service)
  |
  |-- PortfolioStore#findPortfolio(accountId) (infra) ........ Portfolio: USD 5000, no positions
  |
  |-- OrderValidator#validate(request, portfolio)
  |       sell only: enough shares held ................................ no rejection
  |
  |-- BuyingPowerValidator#canAfford(request, portfolio)
  |       250 x 10 = 2500 <= USD 5000 ................................. true
  |
  |-- ExchangeClient#submitOrder(request) (infra, HttpExchangeClient)
  |       → exchange, fill or kill ................................... ExecutionReport.Filled(240, time)
  |
  |-- Order.filled(request, 240, time) (domain) ...................... Order FILLED
  |
  |-- PortfolioUpdater#applyFill(portfolio, order)
  |       USD 5000 - (240 x 10) - 1 fee = 2599
  |       AAPL position: quantity 10, cost basis 2400 ............... new Portfolio (pure, nothing saved yet)
  |
  |-- PortfolioStore#save(newPortfolio) ............................. one transaction
  |
  v
  Order (FILLED)
  v
OrderController
  | switch on order status → FillJson
  v
Client  ← {"status":"FILLED","symbol":"AAPL","side":"BUY","quantity":10,"fillPrice":240,"filledAt":"..."}
```

## Notes

- Affordability is checked on the order's own `limitPrice × quantity` plus the flat fee against the USD balance, before the order is sent: no network call is needed.
- Nothing is saved unless the order was filled. The broker takes the exchange's answer at face value (no sanity check of fills).
- If the exchange times out, the outcome is unknown: 504 and nothing saved. Accepted in the demo; a client order id for idempotent retries would be the real-world fix.
