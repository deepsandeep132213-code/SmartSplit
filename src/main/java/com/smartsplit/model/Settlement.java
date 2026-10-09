package com.smartsplit.model;

import com.smartsplit.util.Money;
import java.math.BigDecimal;

/**
 * A calculated payment: {@code fromUserId} pays {@code toUserId} the given amount.
 * Settlements are calculated, not stored in the database. Immutable.
 */
public record Settlement(int fromUserId, int toUserId, BigDecimal amount) {
    public Settlement {
        if (amount == null) throw new IllegalArgumentException("Settlement amount is required.");
        amount = Money.normalize(amount);
        if (amount.signum() <= 0) throw new IllegalArgumentException("Settlement amount must be greater than zero.");
        if (fromUserId == toUserId) throw new IllegalArgumentException("A user cannot settle with themselves.");
    }
}
