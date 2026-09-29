package com.tddai.broker.infra;

import javax.sql.DataSource;

class JdbcPortfolioStoreTest extends PortfolioStoreContract {
    @Override
    PortfolioStore createStore(DataSource dataSource) {
        return new JdbcPortfolioStore(dataSource);
    }
}
