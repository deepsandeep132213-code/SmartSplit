package com.smartsplit.strategy;

import com.smartsplit.exception.InvalidSplitException;
import com.smartsplit.model.ExpenseSplit;
import com.smartsplit.testrunner.Assertions;
import com.smartsplit.testrunner.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CustomSplitStrategyTest {

    @Test public void valid_exactSum_returnsSplits() throws Exception {
        Map<Integer, BigDecimal> amounts = Map.of(
                1, new BigDecimal("60.00"),
                2, new BigDecimal("40.00"));
        CustomSplitStrategy s = new CustomSplitStrategy(amounts);
        List<ExpenseSplit> splits = s.calculateSplits(new BigDecimal("100.00"), List.of(1, 2));
        Assertions.assertEquals(2, splits.size());
        BigDecimal sum = splits.stream().map(ExpenseSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Assertions.assertDecimalEquals(new BigDecimal("100.00"), sum);
    }

    @Test public void sumMismatch_throwsInvalidSplitException() {
        Map<Integer, BigDecimal> amounts = Map.of(
                1, new BigDecimal("60.00"),
                2, new BigDecimal("30.00")); // only 90, not 100
        CustomSplitStrategy s = new CustomSplitStrategy(amounts);
        Assertions.assertThrows(InvalidSplitException.class, () ->
                s.calculateSplits(new BigDecimal("100.00"), List.of(1, 2)));
    }

    @Test public void missingMember_throwsInvalidSplitException() {
        Map<Integer, BigDecimal> amounts = Map.of(1, new BigDecimal("100.00"));
        CustomSplitStrategy s = new CustomSplitStrategy(amounts);
        Assertions.assertThrows(InvalidSplitException.class, () ->
                s.calculateSplits(new BigDecimal("100.00"), List.of(1, 2)));
    }

    @Test public void extraMember_throwsInvalidSplitException() {
        Map<Integer, BigDecimal> amounts = Map.of(
                1, new BigDecimal("50.00"),
                2, new BigDecimal("50.00"),
                3, new BigDecimal("0.00")); // 3 not in participants
        CustomSplitStrategy s = new CustomSplitStrategy(amounts);
        Assertions.assertThrows(InvalidSplitException.class, () ->
                s.calculateSplits(new BigDecimal("100.00"), List.of(1, 2)));
    }

    @Test public void negativeAmount_throwsInvalidSplitException() {
        Map<Integer, BigDecimal> amounts = Map.of(
                1, new BigDecimal("110.00"),
                2, new BigDecimal("-10.00"));
        CustomSplitStrategy s = new CustomSplitStrategy(amounts);
        Assertions.assertThrows(InvalidSplitException.class, () ->
                s.calculateSplits(new BigDecimal("100.00"), List.of(1, 2)));
    }

    @Test public void threeDecimalAmount_throwsInvalidSplitException() {
        Map<Integer, BigDecimal> amounts = Map.of(
                1, new BigDecimal("50.001"),
                2, new BigDecimal("49.999"));
        CustomSplitStrategy s = new CustomSplitStrategy(amounts);
        Assertions.assertThrows(InvalidSplitException.class, () ->
                s.calculateSplits(new BigDecimal("100.00"), List.of(1, 2)));
    }

    @Test public void zeroAmountAllowed_sumMustStillMatch() throws Exception {
        // One member pays everything, another pays zero (e.g. host pays for a guest)
        Map<Integer, BigDecimal> amounts = Map.of(
                1, new BigDecimal("100.00"),
                2, new BigDecimal("0.00"));
        CustomSplitStrategy s = new CustomSplitStrategy(amounts);
        List<ExpenseSplit> splits = s.calculateSplits(new BigDecimal("100.00"), List.of(1, 2));
        Assertions.assertEquals(2, splits.size());
    }

    @Test public void inputMapImmutable_doesNotAffectStrategy() throws Exception {
        // Defensive copy: mutating original map after construction must not affect calculation
        java.util.Map<Integer, BigDecimal> amounts = new java.util.HashMap<>();
        amounts.put(1, new BigDecimal("100.00"));
        CustomSplitStrategy s = new CustomSplitStrategy(amounts);
        amounts.put(1, new BigDecimal("999.00")); // mutate after construction
        List<ExpenseSplit> splits = s.calculateSplits(new BigDecimal("100.00"), List.of(1));
        Assertions.assertDecimalEquals(new BigDecimal("100.00"), splits.get(0).getAmount());
    }
}
