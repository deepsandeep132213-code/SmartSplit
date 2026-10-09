package com.smartsplit.model;

import java.math.BigDecimal;

/** One member's share of an expense. Maps to the {@code expense_splits} table. */
public class ExpenseSplit {
    private int splitId;
    private int expenseId;
    private int userId;
    private BigDecimal amount;

    public ExpenseSplit() {}
    public ExpenseSplit(int userId, BigDecimal amount) { this.userId = userId; this.amount = amount; }
    public ExpenseSplit(int splitId, int expenseId, int userId, BigDecimal amount) {
        this(userId, amount); this.splitId = splitId; this.expenseId = expenseId;
    }

    public int getSplitId() { return splitId; }
    public void setSplitId(int splitId) { this.splitId = splitId; }
    public int getExpenseId() { return expenseId; }
    public void setExpenseId(int expenseId) { this.expenseId = expenseId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    @Override public String toString() { return "ExpenseSplit{user=" + userId + ", amount=" + amount + "}"; }
}
