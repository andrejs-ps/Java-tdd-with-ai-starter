package com.tddai.broker.infra;

import com.tddai.broker.domain.Quote;
import com.tddai.broker.domain.Symbol;
import com.tddai.broker.domain.order.ExecutionReport;
import com.tddai.broker.domain.order.OrderRequest;

import java.util.Collection;
import java.util.Map;

/**
 * The broker's outbound calls to the exchange: sending orders, and fetching current quotes in one
 * batch.
 */
public interface ExchangeClient {

    /**
     * Sends an order to the exchange and returns what happened to it: executed at a price, or
     * killed.
     * The answer is taken at face value, it is not double-checked.
     *
     * @throws com.tddai.broker.error.ExchangeTimeoutException if the exchange does not answer in
     * time (the outcome is then unknown; nothing is saved)
     *
     * @throws com.tddai.broker.error.ExchangeBadDataException if it answers with an error status
     */
    ExecutionReport submitOrder(OrderRequest order);

    /**
     * Quotes for the requested symbols.
     * Symbols the exchange did not answer for are absent from the map; the caller decides what that
     * means.
     *
     * @throws com.tddai.broker.error.ExchangeTimeoutException if the exchange does not answer in
     * time
     *
     * @throws com.tddai.broker.error.ExchangeBadDataException if it answers with an error status
     */
    Map<Symbol, Quote> getQuotes(Collection<Symbol> symbols);
}
