package com.tddai.broker.service;

import com.tddai.broker.domain.portfolio.CashBalance;
import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.order.Order;
import com.tddai.broker.domain.order.OrderRequest;
import com.tddai.broker.domain.portfolio.Portfolio;
import com.tddai.broker.domain.portfolio.Position;
import com.tddai.broker.domain.order.RejectionReason;
import com.tddai.broker.domain.order.Side;
import com.tddai.broker.domain.Symbol;
import com.tddai.broker.domain.order.TimeInForce;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioUpdaterTest {

    private static final UUID ACCOUNT = UUID.randomUUID();
    private static final Symbol AAPL = Symbol.of("AAPL");

    private final PortfolioUpdater updater = new PortfolioUpdater();

    private static Order filled(Side side, long quantity, String fillPrice) {
        var request = new OrderRequest(ACCOUNT, AAPL, side, quantity, Money.usd("1000"), TimeInForce.FOK);
        return Order.filled(request, Money.usd(fillPrice), Instant.parse("2026-01-02T15:04:05Z"));
    }

    private static Portfolio portfolio(List<CashBalance> cash, List<Position> positions) {
        return new Portfolio(ACCOUNT, cash, positions);
    }

    // ---- BUY

    @Test
    void buyDebitsUsdAndCreatesThePositionWithItsTotalCostBasis() {
        var before = portfolio(List.of(CashBalance.usd("5000")), List.of());

        var after = updater.applyFill(before, filled(Side.BUY, 10, "231.42"));

        assertEquals(CashBalance.usd("2685.80"), after.cashBalance(com.tddai.broker.domain.Currency.USD).orElseThrow());
        assertEquals(new Position(AAPL, 10, Money.usd("2314.20")), after.position(AAPL).orElseThrow());
    }

    @Test
    void buyAddsToAnExistingPositionQuantityAndCostBasis() {
        var before = portfolio(List.of(CashBalance.usd("5000")), List.of(new Position(AAPL, 10, Money.usd("2000"))));

        var after = updater.applyFill(before, filled(Side.BUY, 5, "230.00"));

        assertEquals(new Position(AAPL, 15, Money.usd("3150.00")), after.position(AAPL).orElseThrow());
        assertEquals(CashBalance.usd("3850.00"), after.cashBalance(com.tddai.broker.domain.Currency.USD).orElseThrow());
    }

    @Test
    void buyNeverTouchesTheEurBalance() {
        var before = portfolio(List.of(CashBalance.usd("1000"), CashBalance.eur("200")), List.of());

        var after = updater.applyFill(before, filled(Side.BUY, 10, "60.00"));

        assertEquals(CashBalance.usd("400.00"), after.cashBalance(com.tddai.broker.domain.Currency.USD).orElseThrow());
        assertEquals(CashBalance.eur("200"), after.cashBalance(com.tddai.broker.domain.Currency.EUR).orElseThrow());
    }

    @Test
    void buyWithoutCoveringUsdCannotProduceANegativeBalanceEvenIfEurWouldCoverIt() {
        // not reachable once the order is validated upstream; the CashBalance type is the last line of defence
        var before = portfolio(List.of(CashBalance.usd("100"), CashBalance.eur("10000")), List.of());

        assertThrows(IllegalArgumentException.class, () -> updater.applyFill(before, filled(Side.BUY, 10, "60.00")));
    }

    // ---- SELL

    @Test
    void sellCreditsUsdAndReducesTheCostBasisAtAverageCost() {
        var before = portfolio(List.of(CashBalance.usd("0")), List.of(new Position(AAPL, 10, Money.usd("1000"))));

        var after = updater.applyFill(before, filled(Side.SELL, 4, "150.00"));

        assertEquals(CashBalance.usd("600.00"), after.cashBalance(com.tddai.broker.domain.Currency.USD).orElseThrow());
        assertEquals(new Position(AAPL, 6, Money.usd("600.00")), after.position(AAPL).orElseThrow());
    }

    @Test
    void sellRoundsTheReducedCostBasisHalfEven() {
        // 0.25 x 1/2 = 0.125 -> HALF_EVEN gives 0.12 (HALF_UP would give 0.13)
        var before = portfolio(List.of(CashBalance.usd("0")), List.of(new Position(AAPL, 2, Money.usd("0.25"))));

        var after = updater.applyFill(before, filled(Side.SELL, 1, "10.00"));

        assertEquals(Money.usd("0.12"), after.position(AAPL).orElseThrow().costBasis());
    }

    @Test
    void sellingTheWholePositionRemovesIt() {
        var before = portfolio(List.of(CashBalance.usd("0")), List.of(new Position(AAPL, 10, Money.usd("1000"))));

        var after = updater.applyFill(before, filled(Side.SELL, 10, "150.00"));

        assertTrue(after.position(AAPL).isEmpty());
        assertEquals(CashBalance.usd("1500.00"), after.cashBalance(com.tddai.broker.domain.Currency.USD).orElseThrow());
    }

    @Test
    void sellProceedsNeverGoToEur() {
        var before = portfolio(List.of(CashBalance.usd("0"), CashBalance.eur("50")),
                List.of(new Position(AAPL, 10, Money.usd("1000"))));

        var after = updater.applyFill(before, filled(Side.SELL, 10, "150.00"));

        assertEquals(CashBalance.eur("50"), after.cashBalance(com.tddai.broker.domain.Currency.EUR).orElseThrow());
    }

    @Test
    void sellingMoreThanHeldCannotProduceANegativePosition() {
        // not reachable once OrderValidator runs upstream; the Position type is the last line of defence
        var before = portfolio(List.of(CashBalance.usd("0")), List.of(new Position(AAPL, 3, Money.usd("300"))));

        assertThrows(IllegalArgumentException.class, () -> updater.applyFill(before, filled(Side.SELL, 4, "150.00")));
    }

    // ---- guard

    @Test
    void onlyFilledOrdersCanBeApplied() {
        var request = new OrderRequest(ACCOUNT, AAPL, Side.BUY, 1, Money.usd("100"), TimeInForce.FOK);
        var rejected = Order.rejected(request, RejectionReason.INSUFFICIENT_FUNDS);

        assertThrows(IllegalArgumentException.class,
                () -> updater.applyFill(portfolio(List.of(CashBalance.usd("100")), List.of()), rejected));
    }
}
