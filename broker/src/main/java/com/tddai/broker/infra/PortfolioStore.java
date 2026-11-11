package com.tddai.broker.infra;

import com.tddai.broker.domain.account.Account;
import com.tddai.broker.domain.portfolio.Portfolio;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence of accounts and their portfolios.
 * Every method is one transaction.
 */
public interface PortfolioStore {

    /**
     * Stores a new account together with its initial portfolio.
     */
    void createAccount(Account account, Portfolio initial);

    Optional<Account> findAccount(UUID accountId);

    Optional<Portfolio> findPortfolio(UUID accountId);

    /**
     * Replaces the stored cash balances and positions of the portfolio's account.
     */
    void save(Portfolio portfolio);
}
