package com.tddai.broker.http;

import com.tddai.broker.error.AccountNotFoundException;
import com.tddai.broker.error.BalanceAlreadyOpenException;
import com.tddai.broker.error.BalanceNotFoundException;
import com.tddai.broker.error.ExchangeBadDataException;
import com.tddai.broker.error.ExchangeTimeoutException;
import com.tddai.broker.error.InvalidRequestException;
import io.javalin.config.RoutesConfig;
import io.javalin.http.HttpStatus;
import tools.jackson.core.JacksonException;

import java.util.Map;

/**
 * Maps domain exceptions to status codes.
 * Error body: {"error": "..."}.
 */
public final class ApiErrors {

    private ApiErrors() {
    }

    public static void register(RoutesConfig routes) {
        routes.exception(InvalidRequestException.class, (e, ctx) -> respond(ctx, HttpStatus.BAD_REQUEST, e));
        routes.exception(JacksonException.class, (e, ctx) ->
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Malformed or unreadable JSON body")));
        routes.exception(AccountNotFoundException.class, (e, ctx) -> respond(ctx, HttpStatus.NOT_FOUND, e));
        routes.exception(BalanceNotFoundException.class, (e, ctx) -> respond(ctx, HttpStatus.NOT_FOUND, e));
        routes.exception(BalanceAlreadyOpenException.class, (e, ctx) -> respond(ctx, HttpStatus.CONFLICT, e));
        routes.exception(ExchangeTimeoutException.class, (e, ctx) -> respond(ctx, HttpStatus.GATEWAY_TIMEOUT, e));
        routes.exception(ExchangeBadDataException.class, (e, ctx) -> respond(ctx, HttpStatus.BAD_GATEWAY, e));
    }

    private static void respond(io.javalin.http.Context ctx, HttpStatus status, Exception e) {
        ctx.status(status).json(Map.of("error", String.valueOf(e.getMessage())));
    }
}
