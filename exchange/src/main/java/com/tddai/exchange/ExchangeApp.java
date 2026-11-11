package com.tddai.exchange;

import com.tddai.exchange.config.ExchangeConfig;
import com.tddai.exchange.fault.FaultSimulator;
import com.tddai.exchange.order.OrderExecutor;
import com.tddai.exchange.order.OrderRequestDto;
import com.tddai.exchange.quote.QuoteDto;
import com.tddai.exchange.quote.QuoteGenerator;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson3;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * The simulated NYSE exchange. Two endpoints:
 * GET /quotes?symbols=A,B,C (one or more symbols) and POST /orders (fill or kill, at the same prices as quotes).
 */
public class ExchangeApp {

    private final FaultSimulator faults;
    private final QuoteGenerator quotes;
    private final OrderExecutor executor;

    public ExchangeApp(FaultSimulator faults, QuoteGenerator quotes, OrderExecutor executor) {
        this.faults = faults;
        this.quotes = quotes;
        this.executor = executor;
    }

    /**
     * Creates the exchange and starts it on the configured port.
     * Returns the running server.
     */
    public static Javalin start(ExchangeConfig config) {
        return create(config).start(config.port());
    }

    /**
     * Creates the exchange without starting it.
     */
    public static Javalin create(ExchangeConfig config) {
        Clock clock = Clock.systemUTC();
        var quotes = new QuoteGenerator(clock, config.priceFluctuation(), RandomGenerator.getDefault());
        var app = new ExchangeApp(
                new FaultSimulator(config, RandomGenerator.getDefault()),
                quotes,
                new OrderExecutor(quotes, clock));
        return app.javalin();
    }

    private Javalin javalin() {
        return Javalin.create(cfg -> {
            cfg.startup.showJavalinBanner = false;
            cfg.jsonMapper(new JavalinJackson3(JsonMapper.builder().build(), false));
            cfg.routes.get("/quotes", this::getQuotes);
            cfg.routes.post("/orders", this::postOrder);
            cfg.routes.exception(JacksonException.class, (e, ctx) ->
                    ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Malformed or unreadable JSON body")));
        });
    }

    /**
     * Receives one order and answers with what happened to it.
     * 1) A malformed order (missing or non-positive fields, unknown side) gets a 400
     * 2) Any well-formed order gets a 200 with an execution: FILLED at the listed price, or KILLED with a reason
     */
    private void postOrder(Context ctx) {
        OrderRequestDto order = ctx.bodyAsClass(OrderRequestDto.class);
        String problem = problemWith(order);
        if (problem != null) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", problem));
            return;
        }
        faults.maybeSlowDown();
        ctx.json(executor.execute(order));
    }

    private static String problemWith(OrderRequestDto order) {
        if (order == null) {
            return "order body is required";
        }
        if (order.clientOrderId() == null || order.clientOrderId().isBlank()) {
            return "clientOrderId is required";
        }
        if (order.symbol() == null || order.symbol().isBlank()) {
            return "symbol is required";
        }
        if (order.side() == null) {
            return "side is required (BUY or SELL)";
        }
        if (order.quantity() == null || order.quantity() <= 0) {
            return "quantity must be greater than zero";
        }
        if (order.limitPrice() == null || order.limitPrice().signum() <= 0) {
            return "limitPrice must be greater than zero";
        }
        return null;
    }

    /**
     * 1) Rejects if no `symbols` parameter
     * 2) Appends quotes to JSON only for symbols that are listed in QuoteGenerator
     * @param ctx: Javalin context
     */
    private void getQuotes(Context ctx) {
        String symbolsParam = ctx.queryParam("symbols");
        if (symbolsParam == null || symbolsParam.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error",
                    "symbols query parameter is required. Example: /quotes?symbols=AAPL")
            );
            return;
        }
        faults.maybeSlowDown();

        Map<String, QuoteDto> result = new HashMap<>();
        Arrays.stream(symbolsParam.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(symbol -> quotes.quoteFor(symbol).ifPresent(q -> result.put(symbol, q)));
        ctx.json(result);
    }
}
