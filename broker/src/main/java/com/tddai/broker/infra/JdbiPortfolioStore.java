package com.tddai.broker.infra;

import com.tddai.broker.domain.account.Account;
import com.tddai.broker.domain.portfolio.CashBalance;
import com.tddai.broker.domain.Currency;
import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.portfolio.Portfolio;
import com.tddai.broker.domain.portfolio.Position;
import com.tddai.broker.domain.Symbol;
import com.tddai.broker.domain.account.Investor;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PortfolioStore on Jdbi 3 (core API, fluent SQL; not an ORM).
 * Same behavior as JdbcPortfolioStore.
 */
public class JdbiPortfolioStore implements PortfolioStore {

    private final Jdbi jdbi;

    public JdbiPortfolioStore(DataSource dataSource) {
        this.jdbi = Jdbi.create(dataSource);
    }

    @Override
    public void createAccount(Account account, Portfolio initial) {
        jdbi.useTransaction(handle -> {
            handle.createUpdate("INSERT INTO accounts (account_id, investor_id, investor_name) VALUES (:accountId, :investorId, :investorName)")
                    .bind("accountId", account.id())
                    .bind("investorId", account.investor().id())
                    .bind("investorName", account.investor().name())
                    .execute();
            insertPortfolio(handle, initial);
        });
    }

    @Override
    public Optional<Account> findAccount(UUID accountId) {
        return jdbi.withHandle(handle -> handle
                .createQuery("SELECT account_id, investor_id, investor_name FROM accounts WHERE account_id = :accountId")
                .bind("accountId", accountId)
                .map((rs, ctx) -> new Account(
                        rs.getObject("account_id", UUID.class),
                        new Investor(rs.getObject("investor_id", UUID.class), rs.getString("investor_name"))))
                .findOne());
    }

    @Override
    public Optional<Portfolio> findPortfolio(UUID accountId) {
        return jdbi.withHandle(handle -> {
            boolean exists = handle.createQuery("SELECT COUNT(*) FROM accounts WHERE account_id = :accountId")
                    .bind("accountId", accountId)
                    .mapTo(Long.class)
                    .one() > 0;
            if (!exists) {
                return Optional.<Portfolio>empty();
            }
            List<CashBalance> balances = handle
                    .createQuery("SELECT currency, amount FROM cash_balances WHERE account_id = :accountId ORDER BY currency")
                    .bind("accountId", accountId)
                    .map((rs, ctx) -> new CashBalance(rs.getBigDecimal("amount"), Currency.valueOf(rs.getString("currency"))))
                    .list();
            List<Position> positions = handle
                    .createQuery("SELECT symbol, quantity, cost_basis, cost_currency FROM positions WHERE account_id = :accountId ORDER BY symbol")
                    .bind("accountId", accountId)
                    .map((rs, ctx) -> new Position(
                            Symbol.of(rs.getString("symbol")),
                            rs.getLong("quantity"),
                            new Money(rs.getBigDecimal("cost_basis"), Currency.valueOf(rs.getString("cost_currency")))))
                    .list();
            return Optional.of(new Portfolio(accountId, balances, positions));
        });
    }

    @Override
    public void save(Portfolio portfolio) {
        jdbi.useTransaction(handle -> {
            handle.createUpdate("DELETE FROM cash_balances WHERE account_id = :accountId")
                    .bind("accountId", portfolio.accountId()).execute();
            handle.createUpdate("DELETE FROM positions WHERE account_id = :accountId")
                    .bind("accountId", portfolio.accountId()).execute();
            insertPortfolio(handle, portfolio);
        });
    }

    private static void insertPortfolio(Handle handle, Portfolio portfolio) {
        for (CashBalance balance : portfolio.cashBalances()) {
            handle.createUpdate("INSERT INTO cash_balances (account_id, currency, amount) VALUES (:accountId, :currency, :amount)")
                    .bind("accountId", portfolio.accountId())
                    .bind("currency", balance.currency().name())
                    .bind("amount", balance.amount())
                    .execute();
        }
        for (Position position : portfolio.positions()) {
            handle.createUpdate("INSERT INTO positions (account_id, symbol, quantity, cost_basis, cost_currency) VALUES (:accountId, :symbol, :quantity, :costBasis, :costCurrency)")
                    .bind("accountId", portfolio.accountId())
                    .bind("symbol", position.symbol().value())
                    .bind("quantity", position.quantity())
                    .bind("costBasis", position.costBasis().amount())
                    .bind("costCurrency", position.costBasis().currency().name())
                    .execute();
        }
    }
}
