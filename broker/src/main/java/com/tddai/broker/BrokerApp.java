package com.tddai.broker;

import com.tddai.broker.config.BrokerConfig;
import com.tddai.broker.http.AccountController;
import com.tddai.broker.http.ApiErrors;
import com.tddai.broker.http.OrderController;
import com.tddai.broker.http.PositionController;
import com.tddai.broker.infra.Database;
import com.tddai.broker.infra.ExchangeClient;
import com.tddai.broker.infra.HttpExchangeClient;
import com.tddai.broker.infra.JdbcPortfolioStore;
import com.tddai.broker.infra.PortfolioStore;
import com.tddai.broker.service.AccountService;
import com.tddai.broker.service.BuyingPowerValidator;
import com.tddai.broker.service.OrderService;
import com.tddai.broker.service.OrderValidator;
import com.tddai.broker.service.PortfolioUpdater;
import com.tddai.broker.service.PositionService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson3;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;

/**
 * Wires the broker together.
 * {@link #start} runs it on the configured port; {@link #create} leaves it unstarted.
 */
public final class BrokerApp {

    private BrokerApp() {
    }

    /**
     * Creates the broker and starts it on the configured port.
     * Returns the running server.
     */
    public static Javalin start(BrokerConfig config) {
        return create(config).start(config.port());
    }

    /**
     * Creates the broker without starting it.
     */
    public static Javalin create(BrokerConfig config) {
        DataSource dataSource = Database.create(config.dbUrl());
        PortfolioStore store = new JdbcPortfolioStore(dataSource);
        ExchangeClient exchange = new HttpExchangeClient(
                HttpClient.newBuilder().connectTimeout(config.exchangeTimeout()).build(),
                URI.create(config.exchangeUrl()),
                config.exchangeTimeout());

        var accountController = new AccountController(new AccountService(store));
        var orderController = new OrderController(new OrderService(
                store, new OrderValidator(), new BuyingPowerValidator(), exchange, new PortfolioUpdater()));
        var positionController = new PositionController(new PositionService(store, exchange));

        return Javalin.create(cfg -> {
            cfg.startup.showJavalinBanner = false;
            cfg.jsonMapper(new JavalinJackson3(JsonMapper.builder().build(), false));
            ApiErrors.register(cfg.routes);
            accountController.register(cfg.routes);
            orderController.register(cfg.routes);
            positionController.register(cfg.routes);
        });
    }
}
