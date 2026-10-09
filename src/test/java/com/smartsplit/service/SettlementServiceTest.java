package com.smartsplit.service;

import com.smartsplit.model.Expense;
import com.smartsplit.model.ExpenseCategory;
import com.smartsplit.model.ExpenseSplit;
import com.smartsplit.model.Settlement;
import com.smartsplit.model.SplitType;
import com.smartsplit.testrunner.Assertions;
import com.smartsplit.testrunner.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class SettlementServiceTest {

    private final SettlementService svc = new SettlementService();

    // ── calculateBalances ─────────────────────────────────────────────────

    @Test public void balances_noExpenses_allZero() {
        Map<Integer, BigDecimal> b = svc.calculateBalances(List.of(1, 2, 3), List.of());
        Assertions.assertDecimalEquals(BigDecimal.ZERO, b.get(1));
        Assertions.assertDecimalEquals(BigDecimal.ZERO, b.get(2));
        Assertions.assertDecimalEquals(BigDecimal.ZERO, b.get(3));
    }

    @Test public void balances_oneExpenseEqualSplit_correctBalances() {
        // User 1 paid 90.00 split equally among 1,2,3 (30.00 each)
        Expense e = expense(1, new BigDecimal("90.00"),
                split(1, "30.00"), split(2, "30.00"), split(3, "30.00"));
        Map<Integer, BigDecimal> b = svc.calculateBalances(List.of(1, 2, 3), List.of(e));
        // User 1: paid 90 - owes 30 = +60
        // User 2: paid 0 - owes 30 = -30
        // User 3: paid 0 - owes 30 = -30
        Assertions.assertDecimalEquals(new BigDecimal("60.00"), b.get(1));
        Assertions.assertDecimalEquals(new BigDecimal("-30.00"), b.get(2));
        Assertions.assertDecimalEquals(new BigDecimal("-30.00"), b.get(3));
    }

    @Test public void balances_multipleExpenses_sumsCorrectly() {
        Expense e1 = expense(1, new BigDecimal("60.00"),
                split(1, "30.00"), split(2, "30.00"));
        Expense e2 = expense(2, new BigDecimal("40.00"),
                split(1, "20.00"), split(2, "20.00"));
        Map<Integer, BigDecimal> b = svc.calculateBalances(List.of(1, 2), List.of(e1, e2));
        // User 1: paid 60, owes 30+20=50 → +10
        // User 2: paid 40, owes 30+20=50 → -10
        Assertions.assertDecimalEquals(new BigDecimal("10.00"), b.get(1));
        Assertions.assertDecimalEquals(new BigDecimal("-10.00"), b.get(2));
    }

    // ── calculateSettlements ──────────────────────────────────────────────

    @Test public void settlements_balancedGroup_emptyList() {
        Map<Integer, BigDecimal> b = Map.of(
                1, new BigDecimal("0.00"),
                2, new BigDecimal("0.00"));
        Assertions.assertTrue(svc.calculateSettlements(b).isEmpty());
    }

    @Test public void settlements_twoMembers_onePayment() {
        Map<Integer, BigDecimal> b = Map.of(
                1, new BigDecimal("50.00"),
                2, new BigDecimal("-50.00"));
        List<Settlement> s = svc.calculateSettlements(b);
        Assertions.assertEquals(1, s.size());
        Assertions.assertEquals(2, s.get(0).fromUserId());
        Assertions.assertEquals(1, s.get(0).toUserId());
        Assertions.assertDecimalEquals(new BigDecimal("50.00"), s.get(0).amount());
    }

    @Test public void settlements_threeMembers_atMostNMinusOne() {
        // Classic 3-person case: A paid everything
        Map<Integer, BigDecimal> b = Map.of(
                1, new BigDecimal("200.00"),
                2, new BigDecimal("-100.00"),
                3, new BigDecimal("-100.00"));
        List<Settlement> s = svc.calculateSettlements(b);
        Assertions.assertTrue(s.size() <= 2, "Should need at most n-1 = 2 payments");
        BigDecimal total = s.stream().map(Settlement::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Assertions.assertDecimalEquals(new BigDecimal("200.00"), total);
    }

    @Test public void settlements_sumNotZero_throwsIllegalArgument() {
        Map<Integer, BigDecimal> b = Map.of(
                1, new BigDecimal("50.00"),
                2, new BigDecimal("-30.00")); // doesn't sum to zero
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> svc.calculateSettlements(b));
    }

    @Test public void settlements_greedyMinimisesPayments() {
        // 4 members — greedy should produce 3 payments max
        Map<Integer, BigDecimal> b = Map.of(
                1, new BigDecimal("90.00"),
                2, new BigDecimal("-30.00"),
                3, new BigDecimal("-30.00"),
                4, new BigDecimal("-30.00"));
        List<Settlement> s = svc.calculateSettlements(b);
        Assertions.assertTrue(s.size() <= 3, "Greedy should produce at most 3 payments");
    }

    @Test public void settlements_convenienceOverload_matchesTwoStepVersion() {
        Expense e = expense(1, new BigDecimal("60.00"),
                split(1, "30.00"), split(2, "30.00"));
        List<Settlement> direct = svc.calculateSettlements(List.of(1, 2), List.of(e));
        Assertions.assertEquals(1, direct.size());
        Assertions.assertEquals(2, direct.get(0).fromUserId());
        Assertions.assertDecimalEquals(new BigDecimal("30.00"), direct.get(0).amount());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Expense expense(int paidBy, BigDecimal amount, ExpenseSplit... splits) {
        Expense e = new Expense(1, "test", amount, paidBy,
                ExpenseCategory.OTHER, SplitType.EQUAL, null, null);
        e.setExpenseId(1);
        for (ExpenseSplit s : splits) e.addSplit(s);
        return e;
    }

    private ExpenseSplit split(int userId, String amount) {
        return new ExpenseSplit(userId, new BigDecimal(amount));
    }
}
