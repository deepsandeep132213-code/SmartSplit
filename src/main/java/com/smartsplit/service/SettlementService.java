package com.smartsplit.service;

import com.smartsplit.model.Expense;
import com.smartsplit.model.ExpenseSplit;
import com.smartsplit.model.Settlement;
import com.smartsplit.util.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Calculates per-member balances and the payments that settle them.
 * Pure business logic — no database, GUI or network code.
 */
public class SettlementService {

    public Map<Integer, BigDecimal> calculateBalances(Collection<Integer> memberIds, List<Expense> expenses) {
        Objects.requireNonNull(memberIds); Objects.requireNonNull(expenses);
        Map<Integer, BigDecimal> balances = new LinkedHashMap<>();
        for (Integer id : memberIds) balances.put(id, Money.normalize(BigDecimal.ZERO));
        for (Expense e : expenses) {
            balances.merge(e.getPaidBy(), e.getAmount(), BigDecimal::add);
            for (ExpenseSplit s : e.getSplits())
                balances.merge(s.getUserId(), s.getAmount().negate(), BigDecimal::add);
        }
        balances.replaceAll((id, v) -> Money.normalize(v));
        return balances;
    }

    public List<Settlement> calculateSettlements(Map<Integer, BigDecimal> balances) {
        Objects.requireNonNull(balances);
        BigDecimal total = BigDecimal.ZERO;
        Map<Integer, BigDecimal> credits = new LinkedHashMap<>();
        Map<Integer, BigDecimal> debts   = new LinkedHashMap<>();
        for (Map.Entry<Integer, BigDecimal> e : balances.entrySet()) {
            BigDecimal b = Money.normalize(e.getValue());
            total = total.add(b);
            if (b.signum() > 0) credits.put(e.getKey(), b);
            else if (b.signum() < 0) debts.put(e.getKey(), b.negate());
        }
        if (total.signum() != 0)
            throw new IllegalArgumentException("Balances must add up to zero but add up to " + total + ".");
        List<Settlement> result = new ArrayList<>();
        while (!debts.isEmpty() && !credits.isEmpty()) {
            Map.Entry<Integer, BigDecimal> debtor   = largest(debts);
            Map.Entry<Integer, BigDecimal> creditor = largest(credits);
            BigDecimal payment = debtor.getValue().min(creditor.getValue());
            result.add(new Settlement(debtor.getKey(), creditor.getKey(), payment));
            reduce(debts,   debtor,   payment);
            reduce(credits, creditor, payment);
        }
        return result;
    }

    public List<Settlement> calculateSettlements(Collection<Integer> memberIds, List<Expense> expenses) {
        return calculateSettlements(calculateBalances(memberIds, expenses));
    }

    private static Map.Entry<Integer, BigDecimal> largest(Map<Integer, BigDecimal> m) {
        return m.entrySet().stream()
                .max(Comparator.<Map.Entry<Integer, BigDecimal>, BigDecimal>comparing(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .orElseThrow();
    }

    private static void reduce(Map<Integer, BigDecimal> m, Map.Entry<Integer, BigDecimal> e, BigDecimal p) {
        BigDecimal rem = e.getValue().subtract(p);
        if (rem.signum() == 0) m.remove(e.getKey()); else m.put(e.getKey(), rem);
    }
}
