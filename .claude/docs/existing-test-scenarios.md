# Existing test scenarios

These scenarios exist as part of initial code:

## Unit

- **Order rules**: quantity and limit price must be positive; a sell can't exceed what is held.
- **Buying power**: a buy plus the flat USD broker fee can't cost more than the USD balance (exactly equal is fine, a cent short is refused); EUR is ignored.
- **Portfolio update after a fill**: buy and sell through USD only (sell at average cost), the flat fee on top of a buy and out of a sell's proceeds (waived when the proceeds are smaller than the fee), not part of the cost basis, EUR untouched; balances and positions can never go negative.
- **Order handling** with a fake exchange: filled, killed, refused before sending, exchange failures.
- **FX rates** loading.

## Integration

- **Exchange**: quotes for one or many symbols (unknown symbols left out), bad requests, slow responses; fill-or-kill orders (filled, killed, unlisted symbol, malformed order).
- **Persistence**: the store saves and loads accounts and portfolios correctly.
- **Account set-up**: create an account, open the EUR balance, deposit, and the error cases.

## End-to-end

- **Real exchange (no random slowness)**: fund → buy → sell → valued positions; a buy below the market price is not filled and the account is untouched.
