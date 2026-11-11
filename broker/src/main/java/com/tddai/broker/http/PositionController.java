package com.tddai.broker.http;

import com.tddai.broker.service.PositionService;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.List;

/**
 * Thin HTTP layer for current holdings, valued at live quotes.
 * All amounts are USD.
 */
public class PositionController {

    public record PositionJson(String symbol, long quantity, BigDecimal costBasis,
                               BigDecimal marketValue, BigDecimal unrealizedGainLoss) {
    }

    public record PositionsJson(List<PositionJson> positions) {
    }

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    public void register(RoutesConfig routes) {
        routes.get("/accounts/{accountId}/positions", this::getPositions);
    }

    private void getPositions(Context ctx) {
        var valuations = positionService.getPositions(Requests.accountId(ctx.pathParam("accountId")));
        ctx.json(new PositionsJson(valuations.stream()
                .map(v -> new PositionJson(v.symbol().value(), v.quantity(), Amounts.twoDecimals(v.costBasis().amount()),
                        Amounts.twoDecimals(v.marketValue().amount()),
                        Amounts.twoDecimals(v.unrealizedGainLoss().amount())))
                .toList()));
    }
}
