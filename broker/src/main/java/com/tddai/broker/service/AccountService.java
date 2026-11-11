package com.tddai.broker.service;

import com.tddai.broker.domain.account.Account;
import com.tddai.broker.domain.portfolio.CashBalance;
import com.tddai.broker.domain.Currency;
import com.tddai.broker.domain.portfolio.Portfolio;
import com.tddai.broker.domain.account.Investor;
import com.tddai.broker.error.AccountNotFoundException;
import com.tddai.broker.error.BalanceAlreadyOpenException;
import com.tddai.broker.error.BalanceNotFoundException;
import com.tddai.broker.error.InvalidRequestException;
import com.tddai.broker.infra.PortfolioStore;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Account set-up: create an account, open the extra EUR balance, deposit.
 * Pre-built, not taught.
 */
public class AccountService {

    /**
     * An account together with its current portfolio.
     */
    public record AccountView(Account account, Portfolio portfolio) {
    }

    private final PortfolioStore portfolioStore;

    public AccountService(PortfolioStore portfolioStore) {
        this.portfolioStore = portfolioStore;
    }

    /**
     * One new investor with exactly one account, starting with an empty USD balance.
     */
    public AccountView createAccount(String investorName) {
        if (investorName == null || investorName.isBlank()) {
            throw new InvalidRequestException("investorName is required");
        }
        Account account = new Account(UUID.randomUUID(), new Investor(UUID.randomUUID(), investorName.trim()));
        Portfolio portfolio = new Portfolio(account.id(), List.of(new CashBalance(BigDecimal.ZERO, Currency.USD)), List.of());
        portfolioStore.createAccount(account, portfolio);
        return new AccountView(account, portfolio);
    }

    public AccountView getAccount(UUID accountId) {
        Account account = portfolioStore.findAccount(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));
        Portfolio portfolio = portfolioStore.findPortfolio(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));
        return new AccountView(account, portfolio);
    }

    /**
     * Opens a zero balance.
     * Only EUR can be opened (USD always exists, so opening it is a conflict).
     */
    public AccountView openBalance(UUID accountId, Currency currency) {
        AccountView view = getAccount(accountId);
        if (view.portfolio().cashBalance(currency).isPresent()) {
            throw new BalanceAlreadyOpenException(currency.name());
        }
        if (currency != Currency.EUR) {
            throw new InvalidRequestException("Only an EUR balance can be opened");
        }
        Portfolio updated = view.portfolio().withCashBalance(new CashBalance(BigDecimal.ZERO, currency));
        portfolioStore.save(updated);
        return new AccountView(view.account(), updated);
    }

    public AccountView deposit(UUID accountId, Currency currency, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidRequestException("amount must be greater than zero");
        }
        AccountView view = getAccount(accountId);
        CashBalance balance = view.portfolio().cashBalance(currency)
                .orElseThrow(() -> new BalanceNotFoundException(currency.name()));
        Portfolio updated = view.portfolio().withCashBalance(new CashBalance(balance.amount().add(amount), currency));
        portfolioStore.save(updated);
        return new AccountView(view.account(), updated);
    }
}
