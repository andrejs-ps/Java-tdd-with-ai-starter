package com.tddai.broker.service;

import com.tddai.broker.domain.portfolio.CashBalance;
import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.order.OrderRequest;
import com.tddai.broker.domain.portfolio.Portfolio;
import com.tddai.broker.domain.portfolio.Position;
import com.tddai.broker.domain.order.RejectionReason;
import com.tddai.broker.domain.order.Side;
import com.tddai.broker.domain.Symbol;
import com.tddai.broker.domain.order.TimeInForce;
import com.tddai.broker.error.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderValidatorTest {

    private static final UUID ACCOUNT = UUID.randomUUID();
    private static final Symbol AAPL = Symbol.of("AAPL");

    private final OrderValidator validator = new OrderValidator();

    private static Portfolio portfolioHolding(long aaplQuantity) {
        List<Position> positions = aaplQuantity == 0
                ? List.of()
                : List.of(new Position(AAPL, aaplQuantity, Money.usd("1000")));
        return new Portfolio(ACCOUNT, List.of(CashBalance.usd("0")), positions);
    }

    private static OrderRequest order(Side side, long quantity, String limitPrice) {
        return new OrderRequest(ACCOUNT, AAPL, side, quantity, Money.usd(limitPrice), TimeInForce.FOK);
    }

    @Test
    void acceptsAWellFormedBuyWhateverTheCash() {
        // whether the cash covers it is BuyingPowerValidator's question, not this one's
        var order = order(Side.BUY, 10, "235.00");
        var portfolio = portfolioHolding(0);

        Optional<RejectionReason> expected = Optional.empty();
        var actual = validator.validate(order, portfolio);
        assertEquals(expected, actual);
    }

    @Test
    void acceptsASellOfLessThanHeldWhateverTheCash() {
        var order = order(Side.SELL, 4, "235.00");
        var portfolio = portfolioHolding(10);

        Optional<RejectionReason> expected = Optional.empty();
        var actual = validator.validate(order, portfolio);
        assertEquals(expected, actual);
    }

    @Test
    void acceptsASellOfExactlyTheHeldQuantity() {
        var order = order(Side.SELL, 10, "235.00");
        var portfolio = portfolioHolding(10);

        Optional<RejectionReason> expected = Optional.empty();
        var actual = validator.validate(order, portfolio);
        assertEquals(expected, actual);
    }

    @Test
    void rejectsASellOfMoreThanHeld() {
        var order = order(Side.SELL, 11, "235.00");
        var portfolio = portfolioHolding(10);

        var expected = Optional.of(RejectionReason.INSUFFICIENT_POSITION);
        var actual = validator.validate(order, portfolio);
        assertEquals(expected, actual);
    }

    @Test
    void rejectsASellOfASymbolNotHeldAtAll() {
        var order = order(Side.SELL, 1, "235.00");
        var portfolio = portfolioHolding(0);

        var expected = Optional.of(RejectionReason.INSUFFICIENT_POSITION);
        var actual = validator.validate(order, portfolio);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "quantity={0} limitPrice={1}")
    @CsvSource({
            "0, 235.00",
            "-5, 235.00",
            "1, 0",
            "1, -1.00"
    })
    void rejectsANonPositiveQuantityOrLimitPriceAsInvalid(long quantity, String limitPrice) {
        var order = order(Side.BUY, quantity, limitPrice);
        var portfolio = portfolioHolding(0);

        assertThrows(InvalidRequestException.class, () -> validator.validate(order, portfolio));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"aapl", "TOOLONG"})
    void symbolMustBeAValidTicker(String symbol) {
        assertThrows(InvalidRequestException.class, () -> Symbol.of(symbol));
    }
}
