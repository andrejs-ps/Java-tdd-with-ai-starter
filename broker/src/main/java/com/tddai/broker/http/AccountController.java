package com.tddai.broker.http;

import com.tddai.broker.service.AccountService;
import com.tddai.broker.service.AccountService.AccountView;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static com.tddai.broker.http.Amounts.*;

/**
 * Thin HTTP layer for account set-up.
 * No business logic.
 */
public class AccountController {

    public record CreateAccountBody(String investorName) {
    }

    public record OpenBalanceBody(String currency) {
    }

    public record DepositBody(String currency, BigDecimal amount) {
    }

    public record BalanceJson(String currency, BigDecimal amount) {
    }

    public record AccountJson(String accountId, String investorName, List<BalanceJson> balances) {
    }

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Makes the account web addresses available:
     *  - open a new account,
     *  - find an existing account,
     *  - open a balance in a different currency (default is USD)
     *  - deposit money.
     * Called once when the broker starts.
     */
    public void register(RoutesConfig routes) {
        routes.post("/accounts", this::createAccount);
        routes.get("/accounts/{accountId}", this::getAccount);
        routes.post("/accounts/{accountId}/balances", this::openBalance);
        routes.post("/accounts/{accountId}/deposits", this::deposit);
    }

    private void createAccount(Context ctx) {
        var body = ctx.bodyAsClass(CreateAccountBody.class);
        ctx.status(HttpStatus.CREATED).json(toJson(accountService.createAccount(body.investorName())));
    }

    private void getAccount(Context ctx) {
        ctx.json(toJson(accountService.getAccount(Requests.accountId(ctx.pathParam("accountId")))));
    }

    private void openBalance(Context ctx) {
        var body = ctx.bodyAsClass(OpenBalanceBody.class);
        var view = accountService.openBalance(Requests.accountId(ctx.pathParam("accountId")), Requests.currency(body.currency()));
        ctx.status(HttpStatus.CREATED).json(toJson(view));
    }

    private void deposit(Context ctx) {
        var body = ctx.bodyAsClass(DepositBody.class);
        var view = accountService.deposit(Requests.accountId(ctx.pathParam("accountId")), Requests.currency(body.currency()), body.amount());
        ctx.json(toJson(view));
    }

    private static AccountJson toJson(AccountView view) {
        List<BalanceJson> balances = view.portfolio().cashBalances().stream()
                .map(b -> new BalanceJson(b.currency().name(), twoDecimals(b.amount())))
                .toList();
        return new AccountJson(view.account().id().toString(), view.account().investor().name(), balances);
    }
}
