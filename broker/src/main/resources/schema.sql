CREATE TABLE IF NOT EXISTS accounts (
    account_id  UUID         PRIMARY KEY,
    investor_id   UUID         NOT NULL,
    investor_name VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS cash_balances (
    account_id UUID          NOT NULL REFERENCES accounts (account_id),
    currency   VARCHAR(3)    NOT NULL,
    amount     DECIMAL(24,6) NOT NULL,
    PRIMARY KEY (account_id, currency)
);

CREATE TABLE IF NOT EXISTS positions (
    account_id    UUID          NOT NULL REFERENCES accounts (account_id),
    symbol        VARCHAR(10)   NOT NULL,
    quantity      BIGINT        NOT NULL,
    cost_basis    DECIMAL(24,6) NOT NULL,
    cost_currency VARCHAR(3)    NOT NULL,
    PRIMARY KEY (account_id, symbol)
)
