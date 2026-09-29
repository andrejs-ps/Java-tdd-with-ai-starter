package com.tddai.broker.infra;

import com.tddai.broker.domain.account.Account;
import com.tddai.broker.domain.portfolio.CashBalance;
import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.portfolio.Portfolio;
import com.tddai.broker.domain.portfolio.Position;
import com.tddai.broker.domain.Symbol;
import com.tddai.broker.domain.account.Investor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Every PortfolioStore implementation must pass this.
 * Not a teaching topic; it just keeps the two honest.
 */
abstract class PortfolioStoreContract {

    private PortfolioStore store;
    private final Account account = new Account(UUID.randomUUID(), new Investor(UUID.randomUUID(), "Ada"));

    /**
     * Called once per test with a fresh, empty, schema-initialised database.
     */
    abstract PortfolioStore createStore(DataSource dataSource);

    @BeforeEach
    void setUp() {
        store = createStore(Database.create(Database.uniqueInMemoryUrl()));
    }

    private Portfolio initial() {
        return new Portfolio(account.id(), List.of(CashBalance.usd("0")), List.of());
    }

    @Test
    void anAccountThatWasCreatedCanBeFoundWithItsInvestor() {
        store.createAccount(account, initial());

        assertEquals(Optional.of(account), store.findAccount(account.id()));
    }

    @Test
    void aNewAccountHasItsInitialPortfolio() {
        store.createAccount(account, initial());

        assertEquals(Optional.of(initial()), store.findPortfolio(account.id()));
    }

    @Test
    void anUnknownAccountHasNoAccountAndNoPortfolio() {
        assertEquals(Optional.empty(), store.findAccount(UUID.randomUUID()));
        assertEquals(Optional.empty(), store.findPortfolio(UUID.randomUUID()));
    }

    @Test
    void saveReplacesBalancesAndPositions() {
        store.createAccount(account, initial());
        Portfolio updated = new Portfolio(account.id(),
                List.of(CashBalance.eur("107.4074"), CashBalance.usd("2685.80")),
                List.of(new Position(Symbol.of("AAPL"), 10, Money.usd("2314.20")),
                        new Position(Symbol.of("IBM"), 3, Money.usd("571.65"))));

        store.save(updated);

        assertEquals(Optional.of(updated), store.findPortfolio(account.id()));
    }

    @Test
    void saveCanRemoveAPositionAndLowerABalance() {
        store.createAccount(account, initial());
        store.save(new Portfolio(account.id(), List.of(CashBalance.usd("100")),
                List.of(new Position(Symbol.of("AAPL"), 10, Money.usd("1000")))));
        Portfolio afterSale = new Portfolio(account.id(), List.of(CashBalance.usd("1600")), List.of());

        store.save(afterSale);

        assertEquals(Optional.of(afterSale), store.findPortfolio(account.id()));
    }

    @Test
    void savingOneAccountDoesNotTouchAnother() {
        Account other = new Account(UUID.randomUUID(), new Investor(UUID.randomUUID(), "Grace"));
        store.createAccount(account, initial());
        store.createAccount(other, new Portfolio(other.id(), List.of(CashBalance.usd("42")), List.of()));

        store.save(new Portfolio(account.id(), List.of(CashBalance.usd("1")), List.of()));

        assertEquals(Optional.of(new Portfolio(other.id(), List.of(CashBalance.usd("42")), List.of())),
                store.findPortfolio(other.id()));
    }

    @Test
    void aFailedCreateLeavesNothingBehind() {
        store.createAccount(account, initial());

        assertThrows(RuntimeException.class, () -> store.createAccount(account, initial()));
        assertEquals(Optional.of(initial()), store.findPortfolio(account.id()));
    }
}
