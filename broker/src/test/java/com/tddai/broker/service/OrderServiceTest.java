package com.tddai.broker.service;

import com.tddai.broker.domain.Currency;
import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.Quote;
import com.tddai.broker.domain.Symbol;
import com.tddai.broker.domain.account.Account;
import com.tddai.broker.domain.account.Investor;
import com.tddai.broker.domain.order.ExecutionReport;
import com.tddai.broker.domain.order.KillReason;
import com.tddai.broker.domain.order.Order;
import com.tddai.broker.domain.order.OrderRequest;
import com.tddai.broker.domain.order.OrderStatus;
import com.tddai.broker.domain.order.RejectionReason;
import com.tddai.broker.domain.order.Side;
import com.tddai.broker.domain.order.TimeInForce;
import com.tddai.broker.domain.portfolio.CashBalance;
import com.tddai.broker.domain.portfolio.Portfolio;
import com.tddai.broker.domain.portfolio.Position;
import com.tddai.broker.error.AccountNotFoundException;
import com.tddai.broker.error.ExchangeBadDataException;
import com.tddai.broker.error.ExchangeTimeoutException;
import com.tddai.broker.infra.ExchangeClient;
import com.tddai.broker.support.InMemoryPortfolioStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderServiceTest {

    private static final Symbol AAPL = Symbol.of("AAPL");
    private static final Instant EXCHANGE_TIME = Instant.parse("2026-01-02T15:04:05Z");

    /**
     * Hand-written fake: answers every order with whatever the test set and remembers the orders it got.
     * The exchange alone decides fill or kill; the service takes its answer as is.
     */
    private static class FakeExchange implements ExchangeClient {
        ExecutionReport answer = new ExecutionReport.Killed(KillReason.UNKNOWN_SYMBOL);
        RuntimeException failure;
        final List<OrderRequest> received = new ArrayList<>();

        void fillsAt(String price) {
            answer = new ExecutionReport.Filled(Money.usd(price), EXCHANGE_TIME);
        }

        void kills(KillReason reason) {
            answer = new ExecutionReport.Killed(reason);
        }

        @Override
        public ExecutionReport submitOrder(OrderRequest order) {
            received.add(order);
            if (failure != null) {
                throw failure;
            }
            return answer;
        }

        @Override
        public Map<Symbol, Quote> getQuotes(Collection<Symbol> symbols) {
            throw new UnsupportedOperationException("placing an order never asks for quotes");
        }
    }

    private final InMemoryPortfolioStore store = new InMemoryPortfolioStore();
    private final FakeExchange exchange = new FakeExchange();
    private UUID accountId;
    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(store, new OrderValidator(), new BuyingPowerValidator(Money.usd("1.00")), exchange,
                new PortfolioUpdater(Money.usd("1.00")));
        accountId = UUID.randomUUID();
        store.createAccount(new Account(accountId, new Investor(UUID.randomUUID(), "Ada")),
                new Portfolio(accountId, List.of(CashBalance.usd("5000")), List.of()));
    }

    private OrderRequest order(Side side, long quantity, String limitPrice) {
        return new OrderRequest(accountId, AAPL, side, quantity, Money.usd(limitPrice), TimeInForce.FOK);
    }

    private void holding(long quantity, String costBasis) {
        store.seed(store.findPortfolio(accountId).orElseThrow()
                .withPosition(new Position(AAPL, quantity, Money.usd(costBasis))));
    }

    @Test
    void theOrderIsSentToTheExchangeExactlyAsSubmitted() {
        exchange.fillsAt("231.42");
        OrderRequest request = order(Side.BUY, 10, "235.00");

        service.placeOrder(request);

        assertEquals(List.of(request), exchange.received);
    }

    @Test
    void aFilledBuyIsFilledAtTheExchangePriceAndTime() {
        exchange.fillsAt("231.42");

        Order result = service.placeOrder(order(Side.BUY, 10, "235.00"));

        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(Money.usd("231.42"), result.fillPrice());
        assertEquals(EXCHANGE_TIME, result.filledAt());
    }

    @Test
    void aFilledBuyUpdatesAndSavesThePortfolio() {
        exchange.fillsAt("231.42");

        service.placeOrder(order(Side.BUY, 10, "235.00"));

        Portfolio saved = store.findPortfolio(accountId).orElseThrow();
        assertEquals(CashBalance.usd("2684.80"), saved.cashBalance(Currency.USD).orElseThrow());
        assertEquals(new Position(AAPL, 10, Money.usd("2314.20")), saved.position(AAPL).orElseThrow());
        assertEquals(1, store.saveCount());
    }

    @Test
    void aFilledSellUpdatesAndSavesThePortfolio() {
        holding(10, "1000");
        exchange.fillsAt("150.00");

        Order result = service.placeOrder(order(Side.SELL, 4, "145.00"));

        assertEquals(OrderStatus.FILLED, result.status());
        Portfolio saved = store.findPortfolio(accountId).orElseThrow();
        assertEquals(CashBalance.usd("5599.00"), saved.cashBalance(Currency.USD).orElseThrow());
        assertEquals(6, saved.position(AAPL).orElseThrow().quantity());
    }

    @Test
    void anOrderTheExchangeKillsIsReportedAsKilledWithItsReasonAndNothingIsSaved() {
        exchange.kills(KillReason.LIMIT_NOT_MET);

        Order result = service.placeOrder(order(Side.BUY, 10, "200.00"));

        assertEquals(OrderStatus.KILLED, result.status());
        assertEquals(KillReason.LIMIT_NOT_MET, result.killReason());
        assertNull(result.fillPrice());
        assertEquals(0, store.saveCount());
    }

    @Test
    void anUnknownSymbolKilledByTheExchangeIsReportedAsSuch() {
        exchange.kills(KillReason.UNKNOWN_SYMBOL);

        Order result = service.placeOrder(order(Side.BUY, 1, "100.00"));

        assertEquals(KillReason.UNKNOWN_SYMBOL, result.killReason());
    }

    @Test
    void sellingMoreThanHeldIsRejectedWithoutSendingAnythingToTheExchange() {
        holding(3, "300");

        Order result = service.placeOrder(order(Side.SELL, 4, "145.00"));

        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(RejectionReason.INSUFFICIENT_POSITION, result.rejectionReason());
        assertEquals(0, exchange.received.size());
        assertEquals(0, store.saveCount());
    }

    @Test
    void aBuyTheUsdBalanceCannotAffordIsRejectedWithoutSendingAnythingToTheExchange() {
        // 5000 USD in the account: 30 x 235 = 7050 is too much
        Order result = service.placeOrder(order(Side.BUY, 30, "235.00"));

        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(RejectionReason.INSUFFICIENT_FUNDS, result.rejectionReason());
        assertEquals(0, exchange.received.size());
        assertEquals(0, store.saveCount());
    }

    @Test
    void aBuyCostingTheWholeUsdBalanceIsRejectedBecauseTheFeeIsNotCovered() {
        // 5000 USD in the account: 20 x 250 = 5000 leaves nothing for the 1.00 fee
        Order result = service.placeOrder(order(Side.BUY, 20, "250.00"));

        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(RejectionReason.INSUFFICIENT_FUNDS, result.rejectionReason());
        assertEquals(0, exchange.received.size());
    }

    @Test
    void aSellWithNoUsdCashGoesThroughAndThePortfolioPaysTheFeeFromTheProceeds() {
        store.seed(new Portfolio(accountId, List.of(CashBalance.usd("0")),
                List.of(new Position(AAPL, 10, Money.usd("1000")))));
        exchange.fillsAt("150.00");

        Order result = service.placeOrder(order(Side.SELL, 4, "145.00"));

        assertEquals(OrderStatus.FILLED, result.status());
        Portfolio saved = store.findPortfolio(accountId).orElseThrow();
        assertEquals(CashBalance.usd("599.00"), saved.cashBalance(Currency.USD).orElseThrow());
    }

    @Test
    void anAccountThatDoesNotExistIsNotFound() {
        var unknown = new OrderRequest(UUID.randomUUID(), AAPL, Side.BUY, 1, Money.usd("100"), TimeInForce.FOK);

        assertThrows(AccountNotFoundException.class, () -> service.placeOrder(unknown));
        assertEquals(0, exchange.received.size());
    }

    @Test
    void aTimeoutFromTheExchangePropagatesAndNothingIsSaved() {
        exchange.failure = new ExchangeTimeoutException("slow", null);

        assertThrows(ExchangeTimeoutException.class, () -> service.placeOrder(order(Side.BUY, 1, "235.00")));
        assertEquals(0, store.saveCount());
    }

    @Test
    void unusableExchangeDataPropagatesAndNothingIsSaved() {
        exchange.failure = new ExchangeBadDataException("garbage");

        assertThrows(ExchangeBadDataException.class, () -> service.placeOrder(order(Side.BUY, 1, "235.00")));
        assertEquals(0, store.saveCount());
    }
}
