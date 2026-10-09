package com.smartsplit.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** An expense paid by one member and shared among members. Maps to the {@code expenses} table. */
public class Expense {
    private int expenseId;
    private int groupId;
    private String description;
    private BigDecimal amount;
    private int paidBy;
    private ExpenseCategory category = ExpenseCategory.OTHER;
    private SplitType splitType;
    private LocalDate expenseDate;
    private String notes;
    private LocalDateTime createdAt;
    private final List<ExpenseSplit> splits = new ArrayList<>();

    public Expense() {}

    public Expense(int groupId, String description, BigDecimal amount, int paidBy,
                   ExpenseCategory category, SplitType splitType, LocalDate expenseDate, String notes) {
        this.groupId = groupId; this.description = description; this.amount = amount;
        this.paidBy = paidBy; this.category = category; this.splitType = splitType;
        this.expenseDate = expenseDate; this.notes = notes;
    }

    public int getExpenseId() { return expenseId; }
    public void setExpenseId(int expenseId) { this.expenseId = expenseId; }
    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public int getPaidBy() { return paidBy; }
    public void setPaidBy(int paidBy) { this.paidBy = paidBy; }
    public ExpenseCategory getCategory() { return category; }
    public void setCategory(ExpenseCategory category) { this.category = category; }
    public SplitType getSplitType() { return splitType; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<ExpenseSplit> getSplits() { return Collections.unmodifiableList(splits); }
    public void setSplits(List<ExpenseSplit> newSplits) { splits.clear(); splits.addAll(newSplits); }
    public void addSplit(ExpenseSplit split) { splits.add(split); }
    @Override public String toString() { return "Expense{id=" + expenseId + ", desc='" + description + "', amount=" + amount + "}"; }
}
