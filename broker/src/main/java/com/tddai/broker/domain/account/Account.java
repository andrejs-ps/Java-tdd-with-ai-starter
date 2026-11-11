package com.tddai.broker.domain.account;

import java.util.UUID;

/**
 * One investor, one account.
 * Holds no cash itself: balances live on the Portfolio.
 */
public record Account(UUID id, Investor investor) {
}
