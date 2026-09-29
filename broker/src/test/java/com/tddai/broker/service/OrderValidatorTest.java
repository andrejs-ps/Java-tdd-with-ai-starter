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
        assertEquals(Optional.empty(), validator.validate(order(Side.BUY, 10, "235.00"), portfolioHolding(0)));
    }

    @Test
    void aSellNeedsNoCash() {
        assertEquals(Optional.empty(), validator.validate(order(Side.SELL, 4, "235.00"), portfolioHolding(10)));
    }

    @Test
    void acceptsASellOfLessThanHeld() {
        assertEquals(Optional.empty(), validator.validate(order(Side.SELL, 4, "235.00"), portfolioHolding(10)));
    }

    @Test
    void acceptsASellOfExactlyTheHeldQuantity() {
        assertEquals(Optional.empty(), validator.validate(order(Side.SELL, 10, "235.00"), portfolioHolding(10)));
    }

    @Test
    void rejectsASellOfMoreThanHeld() {
        assertEquals(Optional.of(RejectionReason.INSUFFICIENT_POSITION),
                validator.validate(order(Side.SELL, 11, "235.00"), portfolioHolding(10)));
    }

    @Test
    void rejectsASellOfASymbolNotHeldAtAll() {
        assertEquals(Optional.of(RejectionReason.INSUFFICIENT_POSITION),
                validator.validate(order(Side.SELL, 1, "235.00"), portfolioHolding(0)));
    }

    @Test
    void rejectsZeroQuantityAsInvalid() {
        assertThrows(InvalidRequestException.class,
                () -> validator.validate(order(Side.BUY, 0, "235.00"), portfolioHolding(0)));
    }

    @Test
    void rejectsNegativeQuantityAsInvalid() {
        assertThrows(InvalidRequestException.class,
                () -> validator.validate(order(Side.BUY, -5, "235.00"), portfolioHolding(0)));
    }

    @Test
    void rejectsZeroLimitPriceAsInvalid() {
        assertThrows(InvalidRequestException.class,
                () -> validator.validate(order(Side.BUY, 1, "0"), portfolioHolding(0)));
    }

    @Test
    void rejectsNegativeLimitPriceAsInvalid() {
        assertThrows(InvalidRequestException.class,
                () -> validator.validate(order(Side.BUY, 1, "-1.00"), portfolioHolding(0)));
    }

    @Test
    void symbolMustBeAValidTicker() {
        assertThrows(InvalidRequestException.class, () -> Symbol.of("aapl"));
        assertThrows(InvalidRequestException.class, () -> Symbol.of(""));
        assertThrows(InvalidRequestException.class, () -> Symbol.of("TOOLONG"));
        assertThrows(InvalidRequestException.class, () -> Symbol.of(null));
    }
}
