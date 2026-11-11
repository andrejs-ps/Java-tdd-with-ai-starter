package com.tddai.broker.support;

import io.javalin.Javalin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * A programmable stand-in for the exchange, served over real HTTP so the REAL HttpExchangeClient is
 * exercised.
 * Each test says exactly what comes back from GET /quotes and from POST /orders: a status, a raw
 * body (good, malformed, or oddly shaped) and a delay.
 */
public class StubExchange implements AutoCloseable {

    private static final String TIMESTAMP = "2026-01-02T15:04:05Z";

    private final Javalin app;
    private final List<String> requestedSymbols = new CopyOnWriteArrayList<>();
    private final List<String> receivedOrders = new CopyOnWriteArrayList<>();
    private volatile int quotesStatus = 200;
    private volatile String quotesBody = "{}";
    private volatile int ordersStatus = 200;
    private volatile String ordersBody = "{}";
    private volatile long delayMs = 0;

    public StubExchange() {
        app = Javalin.create(cfg -> {
            cfg.startup.showJavalinBanner = false;
            cfg.routes.get("/quotes", ctx -> {
                requestedSymbols.add(String.valueOf(ctx.queryParam("symbols")));
                pause();
                ctx.status(quotesStatus).contentType("application/json").result(quotesBody);
            });
            cfg.routes.post("/orders", ctx -> {
                receivedOrders.add(ctx.body());
                pause();
                ctx.status(ordersStatus).contentType("application/json").result(ordersBody);
            });
        }).start(0);
    }

    private void pause() throws InterruptedException {
        if (delayMs > 0) {
            Thread.sleep(delayMs);
        }
    }

    public String url() {
        return "http://localhost:" + app.port();
    }

    // ---- GET /quotes

    /**
     * Answer GET /quotes with an exact status and raw body.
     */
    public StubExchange respondWith(int status, String body) {
        this.quotesStatus = status;
        this.quotesBody = body;
        return this;
    }

    /**
     * Answer GET /quotes with well-formed quotes: symbol to price, all stamped with the same
     * timestamp.
     */
    public StubExchange quoting(Map<String, String> pricesBySymbol) {
        return respondWith(200, quotesJson(pricesBySymbol));
    }

    public StubExchange quoting(String symbol, String price) {
        return quoting(Map.of(symbol, price));
    }

    // ---- POST /orders

    /**
     * Answer POST /orders with an exact status and raw body.
     */
    public StubExchange respondToOrdersWith(int status, String body) {
        this.ordersStatus = status;
        this.ordersBody = body;
        return this;
    }

    /**
     * Answer every order with a FILLED execution at this price.
     */
    public StubExchange fillingOrdersAt(String price) {
        return respondToOrdersWith(200, """
                {"orderId": "stub-order", "status": "FILLED", "price": %s, "timestamp": "%s"}
                """.formatted(price, TIMESTAMP));
    }

    /**
     * Answer every order with a KILLED execution and this reason (e.g.
     * LIMIT_NOT_MET, UNKNOWN_SYMBOL).
     */
    public StubExchange killingOrders(String reason) {
        return respondToOrdersWith(200, """
                {"orderId": "stub-order", "status": "KILLED", "reason": "%s", "timestamp": "%s"}
                """.formatted(reason, TIMESTAMP));
    }

    // ---- both

    /**
     * Wait this long before answering either endpoint (use with a short client timeout to trigger a
     * timeout).
     */
    public StubExchange delayResponseBy(long millis) {
        this.delayMs = millis;
        return this;
    }

    /**
     * The raw "symbols" query parameter of every quotes call received so far.
     */
    public List<String> requestedSymbols() {
        return List.copyOf(requestedSymbols);
    }

    /**
     * The raw JSON body of every order received so far.
     */
    public List<String> receivedOrders() {
        return List.copyOf(receivedOrders);
    }

    public static String quotesJson(Map<String, String> pricesBySymbol) {
        return pricesBySymbol.entrySet().stream()
                .map(e -> """
                        "%s": {"price": %s, "timestamp": "%s"}""".formatted(e.getKey(), e.getValue(), TIMESTAMP))
                .collect(Collectors.joining(",", "{", "}"));
    }

    @Override
    public void close() {
        app.stop();
    }
}
