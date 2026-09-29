package com.tddai.broker.http;

import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.order.Order;
import com.tddai.broker.domain.order.OrderRequest;
import com.tddai.broker.domain.order.OrderStatus;
import com.tddai.broker.domain.order.Side;
import com.tddai.broker.domain.Symbol;
import com.tddai.broker.domain.order.TimeInForce;
import com.tddai.broker.service.OrderService;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Thin HTTP layer for orders: parse, delegate to OrderService, map the outcome to a status code.
 */
public class OrderController {

    public record OrderBody(String symbol, String side, Long quantity, BigDecimal limitPrice, String timeInForce) {
    }

    public record FillJson(UUID orderId, String status, String symbol, String side, long quantity, BigDecimal fillPrice, Instant filledAt) {
    }

    public record KilledJson(UUID orderId, String status, String symbol, String side, long quantity, String reason) {
    }

    public record RejectionJson(UUID orderId, String status, String reason) {
    }

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public void register(RoutesConfig routes) {
        routes.post("/accounts/{accountId}/orders", this::placeOrder);
    }

    private void placeOrder(Context ctx) {
        var accountId = Requests.accountId(ctx.pathParam("accountId"));
        var body = ctx.bodyAsClass(OrderBody.class);

        TimeInForce timeInForce = body.timeInForce() == null
                ? TimeInForce.FOK
                : Requests.enumValue(TimeInForce.class, body.timeInForce(), "timeInForce");
        var request = new OrderRequest(
                accountId,
                Symbol.of(body.symbol()),
                Requests.enumValue(Side.class, body.side(), "side"),
                Requests.required(body.quantity(), "quantity"),
                Money.usd(Requests.required(body.limitPrice(), "limitPrice")),
                timeInForce);

        Order order = orderService.placeOrder(request);

        // 200: the order reached the exchange and got an answer, filled or killed (a normal outcome for fill or kill).
        // 422: the broker refused to send it at all.
        switch (order.status()) {
            case FILLED -> ctx.json(new FillJson(order.id(), OrderStatus.FILLED.name(), order.symbol().value(),
                    order.side().name(), order.quantity(), order.fillPrice().amount(), order.filledAt()));
            case KILLED -> ctx.json(new KilledJson(order.id(), OrderStatus.KILLED.name(), order.symbol().value(),
                    order.side().name(), order.quantity(), order.killReason().name()));
            case REJECTED -> ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
                    .json(new RejectionJson(order.id(), OrderStatus.REJECTED.name(), order.rejectionReason().name()));
        }
    }
}
