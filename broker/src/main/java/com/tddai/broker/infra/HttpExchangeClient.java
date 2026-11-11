package com.tddai.broker.infra;

import com.tddai.broker.domain.Money;
import com.tddai.broker.domain.Quote;
import com.tddai.broker.domain.Symbol;
import com.tddai.broker.domain.order.ExecutionReport;
import com.tddai.broker.domain.order.KillReason;
import com.tddai.broker.domain.order.OrderRequest;
import com.tddai.broker.error.ExchangeBadDataException;
import com.tddai.broker.error.ExchangeTimeoutException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ExchangeClient over java.net.http: POST {baseUri}/orders to send an order, GET
 * {baseUri}/quotes?symbols=A,B,C for quotes.
 * Prices are USD.
 */
public class HttpExchangeClient implements ExchangeClient {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    private final HttpClient httpClient;
    private final URI baseUri;
    private final Duration timeout;

    public HttpExchangeClient(HttpClient httpClient, URI baseUri, Duration timeout) {
        this.httpClient = httpClient;
        this.baseUri = baseUri;
        this.timeout = timeout;
    }

    @Override
    public ExecutionReport submitOrder(OrderRequest order) {
        String body = MAPPER.writeValueAsString(Map.of(
                "clientOrderId", order.id().toString(),
                "symbol", order.symbol().value(),
                "side", order.side().name(),
                "quantity", order.quantity(),
                "limitPrice", order.limitPrice().amount(),
                "timeInForce", order.timeInForce().name()));
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUri + "/orders"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new ExchangeBadDataException("Exchange answered with status " + response.statusCode());
        }
        ExecutionBody execution = MAPPER.readValue(response.body(), ExecutionBody.class);
        return switch (execution.status()) {
            case "FILLED" -> new ExecutionReport.Filled(Money.usd(execution.price()), execution.timestamp());
            case "KILLED" -> new ExecutionReport.Killed(KillReason.valueOf(execution.reason()));
            default -> throw new IllegalStateException("Unknown order status: " + execution.status());
        };
    }

    @Override
    public Map<Symbol, Quote> getQuotes(Collection<Symbol> symbols) {
        String symbolList = symbols.stream().map(Symbol::value).sorted().collect(Collectors.joining(","));
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUri + "/quotes?symbols=" + symbolList))
                .timeout(timeout)
                .GET()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new ExchangeBadDataException("Exchange answered with status " + response.statusCode());
        }
        Map<String, QuoteBody> answer = MAPPER.readValue(response.body(), new TypeReference<>() { });

        Map<Symbol, Quote> quotes = new LinkedHashMap<>();
        for (Symbol symbol : symbols) {
            QuoteBody quote = answer.get(symbol.value());
            if (quote != null) {
                quotes.put(symbol, new Quote(symbol, Money.usd(quote.price()), quote.timestamp()));
            }
        }
        return quotes;
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new ExchangeTimeoutException("Exchange did not answer within " + timeout, e);
        } catch (IOException e) {
            throw new ExchangeBadDataException("Exchange unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExchangeBadDataException("Interrupted while waiting for the exchange", e);
        }
    }

    /**
     * The exchange's answer to an order: price and timestamp when FILLED, reason when KILLED.
     */
    private record ExecutionBody(String status, BigDecimal price, Instant timestamp, String reason) {
    }

    private record QuoteBody(BigDecimal price, Instant timestamp) {
    }
}
