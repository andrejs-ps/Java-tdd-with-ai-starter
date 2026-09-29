package com.tddai.broker.infra;

import javax.sql.DataSource;

class JdbiPortfolioStoreTest extends PortfolioStoreContract {
    @Override
    PortfolioStore createStore(DataSource dataSource) {
        return new JdbiPortfolioStore(dataSource);
    }
}
