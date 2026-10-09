package com.smartsplit.strategy;

import com.smartsplit.exception.InvalidSplitException;
import com.smartsplit.model.ExpenseSplit;
import com.smartsplit.testrunner.Assertions;
import com.smartsplit.testrunner.Test;
import java.math.BigDecimal;
import java.util.List;

public class EqualSplitStrategyTest {

    private final EqualSplitStrategy strategy = new EqualSplitStrategy();

    @Test public void twoMembers_evenAmount_equalShares() throws Exception {
        List<ExpenseSplit> splits = strategy.calculateSplits(
                new BigDecimal("100.00"), List.of(1, 2));
        Assertions.assertEquals(2, splits.size());
        Assertions.assertDecimalEquals(new BigDecimal("50.00"), splits.get(0).getAmount());
        Assertions.assertDecimalEquals(new BigDecimal("50.00"), splits.get(1).getAmount());
    }

    @Test public void threeMembers_remainder_extraPaisaFirstMember() throws Exception {
        // 100.00 / 3 = 33.33 + 33.33 + 33.34 → first member gets the extra paisa
        List<ExpenseSplit> splits = strategy.calculateSplits(
                new BigDecimal("100.00"), List.of(1, 2, 3));
        BigDecimal sum = splits.stream().map(ExpenseSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Assertions.assertDecimalEquals(new BigDecimal("100.00"), sum);
        // First member should have the rounded-up share
        Assertions.assertDecimalEquals(new BigDecimal("33.34"), splits.get(0).getAmount());
        Assertions.assertDecimalEquals(new BigDecimal("33.33"), splits.get(1).getAmount());
        Assertions.assertDecimalEquals(new BigDecimal("33.33"), splits.get(2).getAmount());
    }

    @Test public void oneMember_fullAmount() throws Exception {
        List<ExpenseSplit> splits = strategy.calculateSplits(
                new BigDecimal("75.50"), List.of(42));
        Assertions.assertEquals(1, splits.size());
        Assertions.assertDecimalEquals(new BigDecimal("75.50"), splits.get(0).getAmount());
    }

    @Test public void fiveMembers_oddAmount_sumsExactly() throws Exception {
        // 10.00 / 5 = 2.00 each
        List<ExpenseSplit> splits = strategy.calculateSplits(
                new BigDecimal("10.00"), List.of(1, 2, 3, 4, 5));
        BigDecimal sum = splits.stream().map(ExpenseSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Assertions.assertDecimalEquals(new BigDecimal("10.00"), sum);
    }

    @Test public void sevenMembers_large_sumsExactly() throws Exception {
        // 100.00 / 7: any distribution must sum to exactly 100.00
        List<ExpenseSplit> splits = strategy.calculateSplits(
                new BigDecimal("100.00"), List.of(1, 2, 3, 4, 5, 6, 7));
        BigDecimal sum = splits.stream().map(ExpenseSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Assertions.assertDecimalEquals(new BigDecimal("100.00"), sum);
    }

    @Test public void nullTotal_throwsInvalidSplitException() {
        Assertions.assertThrows(InvalidSplitException.class, () ->
                strategy.calculateSplits(null, List.of(1, 2)));
    }

    @Test public void zeroTotal_throwsInvalidSplitException() {
        Assertions.assertThrows(InvalidSplitException.class, () ->
                strategy.calculateSplits(BigDecimal.ZERO, List.of(1, 2)));
    }

    @Test public void negativeTotal_throwsInvalidSplitException() {
        Assertions.assertThrows(InvalidSplitException.class, () ->
                strategy.calculateSplits(new BigDecimal("-10.00"), List.of(1, 2)));
    }

    @Test public void emptyParticipants_throwsInvalidSplitException() {
        Assertions.assertThrows(InvalidSplitException.class, () ->
                strategy.calculateSplits(new BigDecimal("50.00"), List.of()));
    }

    @Test public void duplicateParticipant_throwsInvalidSplitException() {
        Assertions.assertThrows(InvalidSplitException.class, () ->
                strategy.calculateSplits(new BigDecimal("50.00"), List.of(1, 1)));
    }

    @Test public void threeDecimalTotal_throwsInvalidSplitException() {
        Assertions.assertThrows(InvalidSplitException.class, () ->
                strategy.calculateSplits(new BigDecimal("10.005"), List.of(1, 2)));
    }

    @Test public void userIdsPreservedInSplits() throws Exception {
        List<ExpenseSplit> splits = strategy.calculateSplits(
                new BigDecimal("60.00"), List.of(5, 10, 15));
        Assertions.assertEquals(5,  splits.get(0).getUserId());
        Assertions.assertEquals(10, splits.get(1).getUserId());
        Assertions.assertEquals(15, splits.get(2).getUserId());
    }
}
