package com.smartsplit.strategy;

import com.smartsplit.exception.InvalidSplitException;
import com.smartsplit.model.ExpenseSplit;
import com.smartsplit.model.SplitType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits an expense equally. Works in whole paise so shares always add up exactly.
 * Leftover paise are distributed one each to the first members in the list.
 */
public class EqualSplitStrategy implements SplitStrategy {

    @Override public SplitType getSplitType() { return SplitType.EQUAL; }

    @Override
    public List<ExpenseSplit> calculateSplits(BigDecimal total, List<Integer> participantIds)
            throws InvalidSplitException {
        SplitValidator.validateTotal(total);
        SplitValidator.validateParticipants(participantIds);
        BigInteger totalPaise = total.movePointRight(2).toBigIntegerExact();
        BigInteger[] qr = totalPaise.divideAndRemainder(BigInteger.valueOf(participantIds.size()));
        int extra = qr[1].intValueExact();
        List<ExpenseSplit> splits = new ArrayList<>();
        for (int i = 0; i < participantIds.size(); i++) {
            BigInteger paise = i < extra ? qr[0].add(BigInteger.ONE) : qr[0];
            splits.add(new ExpenseSplit(participantIds.get(i), new BigDecimal(paise, 2)));
        }
        return splits;
    }
}
