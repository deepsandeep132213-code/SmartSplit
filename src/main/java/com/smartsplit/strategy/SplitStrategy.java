package com.smartsplit.strategy;

import com.smartsplit.exception.InvalidSplitException;
import com.smartsplit.model.ExpenseSplit;
import com.smartsplit.model.SplitType;
import java.math.BigDecimal;
import java.util.List;

/** Strategy interface for dividing an expense between members. */
public interface SplitStrategy {
    SplitType getSplitType();
    List<ExpenseSplit> calculateSplits(BigDecimal total, List<Integer> participantIds)
            throws InvalidSplitException;
}
