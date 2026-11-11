package com.tddai.exchange.order;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The exchange's answer to an order it received (what real exchanges call an execution report).
 * <ul>
 *   <li>orderId: the exchange's own reference for this order</li>
 *   <li>clientOrderId: the sender's reference, echoed back unchanged</li>
 *   <li>status: FILLED or KILLED</li>
 *   <li>symbol, side, quantity: the order as the exchange understood it</li>
 *   <li>price: the USD execution price; present only when FILLED</li>
 *   <li>reason: present only when KILLED</li>
 *   <li>timestamp: when the exchange decided</li>
 * </ul>
 * Fields that do not apply are left out of the JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExecutionDto(
        String orderId,
        String clientOrderId,
        ExecutionStatus status,
        String symbol,
        Side side,
        long quantity,
        BigDecimal price,
        KillReason reason,
        Instant timestamp) {
}
