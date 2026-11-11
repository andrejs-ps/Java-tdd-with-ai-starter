package com.tddai.broker.service;

import com.tddai.broker.domain.portfolio.Portfolio;
import com.tddai.broker.domain.portfolio.Position;
import com.tddai.broker.domain.portfolio.PositionValuation;
import com.tddai.broker.domain.Quote;
import com.tddai.broker.domain.Symbol;
import com.tddai.broker.error.AccountNotFoundException;
import com.tddai.broker.error.ExchangeBadDataException;
import com.tddai.broker.infra.ExchangeClient;
import com.tddai.broker.infra.PortfolioStore;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Values the stored positions at live quotes, fetched in ONE batch call.
 * Nothing is cached or stored.
 */
public class PositionService {

    private final PortfolioStore portfolioStore;
    private final ExchangeClient exchangeClient;

    public PositionService(PortfolioStore portfolioStore, ExchangeClient exchangeClient) {
        this.portfolioStore = portfolioStore;
        this.exchangeClient = exchangeClient;
    }

    public List<PositionValuation> getPositions(UUID accountId) {
        Portfolio portfolio = portfolioStore.findPortfolio(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));
        if (portfolio.positions().isEmpty()) {
            return List.of();
        }

        List<Symbol> symbols = portfolio.positions().stream().map(Position::symbol).toList();
        Map<Symbol, Quote> quotes = exchangeClient.getQuotes(symbols);

        return portfolio.positions().stream().map(position -> {
            Quote quote = quotes.get(position.symbol());
            if (quote == null) {
                throw new ExchangeBadDataException("Exchange returned no quote for " + position.symbol());
            }
            var marketValue = quote.price().times(position.quantity());
            return new PositionValuation(position.symbol(), position.quantity(), position.costBasis(),
                    marketValue, marketValue.minus(position.costBasis()));
        }).toList();
    }
}
