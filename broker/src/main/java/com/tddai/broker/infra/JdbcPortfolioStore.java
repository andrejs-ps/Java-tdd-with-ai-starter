package com.tddai.broker.infra;

import com.tddai.broker.domain.account.Account;
import com.tddai.broker.domain.portfolio.CashBalance;
import com.tddai.broker.domain.Currency;
import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.portfolio.Portfolio;
import com.tddai.broker.domain.portfolio.Position;
import com.tddai.broker.domain.Symbol;
import com.tddai.broker.domain.account.Investor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PortfolioStore on plain JDBC.
 */
public class JdbcPortfolioStore implements PortfolioStore {

    private final DataSource dataSource;

    public JdbcPortfolioStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createAccount(Account account, Portfolio initial) {
        inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO accounts (account_id, investor_id, investor_name) VALUES (?, ?, ?)")) {
                ps.setObject(1, account.id());
                ps.setObject(2, account.investor().id());
                ps.setString(3, account.investor().name());
                ps.executeUpdate();
            }
            insertPortfolio(connection, initial);
        });
    }

    @Override
    public Optional<Account> findAccount(UUID accountId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT account_id, investor_id, investor_name FROM accounts WHERE account_id = ?")) {
            ps.setObject(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Account(
                        rs.getObject("account_id", UUID.class),
                        new Investor(rs.getObject("investor_id", UUID.class), rs.getString("investor_name"))));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("findAccount failed", e);
        }
    }

    @Override
    public Optional<Portfolio> findPortfolio(UUID accountId) {
        try (Connection connection = dataSource.getConnection()) {
            if (findAccount(accountId).isEmpty()) {
                return Optional.empty();
            }
            List<CashBalance> balances = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT currency, amount FROM cash_balances WHERE account_id = ? ORDER BY currency")) {
                ps.setObject(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        balances.add(new CashBalance(rs.getBigDecimal("amount"), Currency.valueOf(rs.getString("currency"))));
                    }
                }
            }
            List<Position> positions = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT symbol, quantity, cost_basis, cost_currency FROM positions WHERE account_id = ? ORDER BY symbol")) {
                ps.setObject(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        positions.add(new Position(
                                Symbol.of(rs.getString("symbol")),
                                rs.getLong("quantity"),
                                new Money(rs.getBigDecimal("cost_basis"), Currency.valueOf(rs.getString("cost_currency")))));
                    }
                }
            }
            return Optional.of(new Portfolio(accountId, balances, positions));
        } catch (SQLException e) {
            throw new IllegalStateException("findPortfolio failed", e);
        }
    }

    @Override
    public void save(Portfolio portfolio) {
        inTransaction(connection -> {
            for (String table : List.of("cash_balances", "positions")) {
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM " + table + " WHERE account_id = ?")) {
                    ps.setObject(1, portfolio.accountId());
                    ps.executeUpdate();
                }
            }
            insertPortfolio(connection, portfolio);
        });
    }

    private static void insertPortfolio(Connection connection, Portfolio portfolio) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO cash_balances (account_id, currency, amount) VALUES (?, ?, ?)")) {
            for (CashBalance balance : portfolio.cashBalances()) {
                ps.setObject(1, portfolio.accountId());
                ps.setString(2, balance.currency().name());
                ps.setBigDecimal(3, balance.amount());
                ps.executeUpdate();
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO positions (account_id, symbol, quantity, cost_basis, cost_currency) VALUES (?, ?, ?, ?, ?)")) {
            for (Position position : portfolio.positions()) {
                ps.setObject(1, portfolio.accountId());
                ps.setString(2, position.symbol().value());
                ps.setLong(3, position.quantity());
                ps.setBigDecimal(4, position.costBasis().amount());
                ps.setString(5, position.costBasis().currency().name());
                ps.executeUpdate();
            }
        }
    }

    private interface SqlWork {
        void run(Connection connection) throws SQLException;
    }

    private void inTransaction(SqlWork work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                work.run(connection);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Transaction failed", e);
        }
    }
}
