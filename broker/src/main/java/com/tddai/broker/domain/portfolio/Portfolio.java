package com.tddai.broker.domain.portfolio;

import com.tddai.broker.domain.Currency;
import com.tddai.broker.domain.Symbol;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The one piece of persisted, mutated state: an account's cash balances and positions.
 * Immutable.
 */
public record Portfolio(UUID accountId, List<CashBalance> cashBalances, List<Position> positions) {

    public Portfolio {
        cashBalances = List.copyOf(cashBalances);
        positions = List.copyOf(positions);
    }

    /**
     * Answers "is this balance open, and what is it?"
     * The account's cash balance in this currency, if the account has one open.
     * An account always has USD; EUR only after it has been opened.
     */
    public Optional<CashBalance> cashBalance(Currency currency) {
        return cashBalances.stream().filter(b -> b.currency() == currency).findFirst();
    }

    /**
     * Answers "how much money is there?"
     * How much money the account holds in this currency.
     * Zero when no balance in that currency has been opened.
     */
    public BigDecimal cash(Currency currency) {
        return cashBalance(currency).map(CashBalance::amount).orElse(BigDecimal.ZERO);
    }

    /**
     * The shares the account holds in this stock, if any.
     */
    public Optional<Position> position(Symbol symbol) {
        return positions.stream()
                .filter(p -> p.symbol().equals(symbol))
                .findFirst();
    }

    /**
     * A copy of this portfolio with the cash balance in that currency replaced by the given one.
     * The original portfolio is not changed.
     */
    public Portfolio withCashBalance(CashBalance balance) {
        List<CashBalance> updated = new ArrayList<>(cashBalances);
        updated.removeIf(b -> b.currency() == balance.currency());
        updated.add(balance);
        return new Portfolio(accountId, updated, positions);
    }

    /**
     * A copy of this portfolio where the holding in that stock is replaced by the given one,
     * or added if the account did not hold the stock yet.
     * The original portfolio is not changed.
     */
    public Portfolio withPosition(Position position) {
        List<Position> updated = new ArrayList<>(positions);
        updated.removeIf(p -> p.symbol().equals(position.symbol()));
        updated.add(position);
        return new Portfolio(accountId, cashBalances, updated);
    }

    /**
     * A copy of this portfolio with the holding in that stock removed, for example after the last
     * share has been sold.
     * The original portfolio is not changed.
     */
    public Portfolio withoutPosition(Symbol symbol) {
        List<Position> updated = new ArrayList<>(positions);
        updated.removeIf(p -> p.symbol().equals(symbol));
        return new Portfolio(accountId, cashBalances, updated);
    }
}
