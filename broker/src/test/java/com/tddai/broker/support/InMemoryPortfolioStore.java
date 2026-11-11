package com.tddai.broker.support;

import com.tddai.broker.domain.account.Account;
import com.tddai.broker.domain.portfolio.Portfolio;
import com.tddai.broker.infra.PortfolioStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Hand-written fake for unit tests: a map, plus a count of saves so tests can assert "nothing was
 * saved".
 */
public class InMemoryPortfolioStore implements PortfolioStore {

    private final Map<UUID, Account> accounts = new HashMap<>();
    private final Map<UUID, Portfolio> portfolios = new HashMap<>();
    private int saveCount;

    @Override
    public void createAccount(Account account, Portfolio initial) {
        accounts.put(account.id(), account);
        portfolios.put(account.id(), initial);
    }

    @Override
    public Optional<Account> findAccount(UUID accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    @Override
    public Optional<Portfolio> findPortfolio(UUID accountId) {
        return Optional.ofNullable(portfolios.get(accountId));
    }

    @Override
    public void save(Portfolio portfolio) {
        saveCount++;
        portfolios.put(portfolio.accountId(), portfolio);
    }

    /**
     * Sets up a starting portfolio without counting as a save.
     */
    public void seed(Portfolio portfolio) {
        portfolios.put(portfolio.accountId(), portfolio);
    }

    public int saveCount() {
        return saveCount;
    }
}
