package com.smartsplit.strategy;

import com.smartsplit.exception.InvalidSplitException;
import com.smartsplit.util.Money;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shared validation logic for all split strategies. */
final class SplitValidator {
    private SplitValidator() {}

    static void validateTotal(BigDecimal total) throws InvalidSplitException {
        if (total == null) throw new InvalidSplitException("The expense amount is required.");
        if (total.signum() <= 0) throw new InvalidSplitException("The expense amount must be greater than zero.");
        if (!Money.hasValidScale(total)) throw new InvalidSplitException("The expense amount can have at most 2 decimal places.");
    }

    static void validateParticipants(List<Integer> ids) throws InvalidSplitException {
        if (ids == null || ids.isEmpty()) throw new InvalidSplitException("Select at least one member.");
        Set<Integer> seen = new HashSet<>();
        for (Integer id : ids) {
            if (id == null) throw new InvalidSplitException("A selected member is not valid.");
            if (!seen.add(id)) throw new InvalidSplitException("The same member was selected more than once.");
        }
    }
}
