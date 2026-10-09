package com.smartsplit.strategy;

import com.smartsplit.exception.InvalidSplitException;
import com.smartsplit.model.ExpenseSplit;
import com.smartsplit.model.SplitType;
import com.smartsplit.util.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Splits an expense using caller-specified amounts per member. */
public class CustomSplitStrategy implements SplitStrategy {

    private final Map<Integer, BigDecimal> amountsByUserId;

    public CustomSplitStrategy(Map<Integer, BigDecimal> amountsByUserId) {
        this.amountsByUserId = new LinkedHashMap<>(Objects.requireNonNull(amountsByUserId));
    }

    @Override public SplitType getSplitType() { return SplitType.CUSTOM; }

    @Override
    public List<ExpenseSplit> calculateSplits(BigDecimal total, List<Integer> participantIds)
            throws InvalidSplitException {
        SplitValidator.validateTotal(total);
        SplitValidator.validateParticipants(participantIds);
        checkSameMembers(participantIds);
        List<ExpenseSplit> splits = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (Integer userId : participantIds) {
            BigDecimal amount = amountsByUserId.get(userId);
            validateAmount(userId, amount);
            sum = sum.add(amount);
            splits.add(new ExpenseSplit(userId, Money.normalize(amount)));
        }
        if (sum.compareTo(total) != 0) {
            throw new InvalidSplitException(String.format(
                    "The custom amounts add up to %s but the expense total is %s (difference %s).",
                    Money.normalize(sum).toPlainString(), Money.normalize(total).toPlainString(),
                    Money.normalize(total.subtract(sum).abs()).toPlainString()));
        }
        return splits;
    }

    private void checkSameMembers(List<Integer> participantIds) throws InvalidSplitException {
        Set<Integer> participants = new HashSet<>(participantIds);
        Set<Integer> missing = new HashSet<>(participants); missing.removeAll(amountsByUserId.keySet());
        if (!missing.isEmpty()) throw new InvalidSplitException("No amount entered for member id(s): " + missing + ".");
        Set<Integer> extra = new HashSet<>(amountsByUserId.keySet()); extra.removeAll(participants);
        if (!extra.isEmpty()) throw new InvalidSplitException("Amount entered for non-participant id(s): " + extra + ".");
    }

    private void validateAmount(Integer userId, BigDecimal amount) throws InvalidSplitException {
        if (amount == null) throw new InvalidSplitException("No amount entered for member id " + userId + ".");
        if (amount.signum() < 0) throw new InvalidSplitException("Amounts cannot be negative (member id " + userId + ").");
        if (!Money.hasValidScale(amount)) throw new InvalidSplitException("Amounts can have at most 2 decimal places (member id " + userId + ").");
    }
}
