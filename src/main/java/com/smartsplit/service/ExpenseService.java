package com.smartsplit.service;

import com.smartsplit.dao.ExpenseDAO;
import com.smartsplit.dao.GroupDAO;
import com.smartsplit.exception.AuthenticationException;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.exception.InvalidExpenseException;
import com.smartsplit.exception.InvalidSplitException;
import com.smartsplit.exception.MemberNotFoundException;
import com.smartsplit.model.Expense;
import com.smartsplit.model.ExpenseCategory;
import com.smartsplit.model.ExpenseSplit;
import com.smartsplit.model.Settlement;
import com.smartsplit.model.SplitType;
import com.smartsplit.model.User;
import com.smartsplit.strategy.CustomSplitStrategy;
import com.smartsplit.strategy.EqualSplitStrategy;
import com.smartsplit.strategy.SplitStrategy;
import com.smartsplit.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Business logic for creating, reading and deleting expenses, and computing balances/settlements.
 * Delegates split calculation to a SplitStrategy; delegates settlement to SettlementService.
 */
public class ExpenseService {

    private final ExpenseDAO        expenseDao;
    private final GroupDAO          groupDao;
    private final AuthenticationService auth;
    private final SettlementService settlement;

    public ExpenseService(ExpenseDAO expenseDao, GroupDAO groupDao,
                          AuthenticationService auth, SettlementService settlement) {
        this.expenseDao = Objects.requireNonNull(expenseDao);
        this.groupDao   = Objects.requireNonNull(groupDao);
        this.auth       = Objects.requireNonNull(auth);
        this.settlement = Objects.requireNonNull(settlement);
    }

    // ── Add expense (equal split) ──────────────────────────────────────────

    /**
     * Adds an expense split equally among all current group members.
     */
    public Expense addExpense(int groupId, String description, BigDecimal amount,
                              int paidByUserId, ExpenseCategory category,
                              LocalDate expenseDate, String notes)
            throws AuthenticationException, DatabaseException,
                   InvalidExpenseException, InvalidSplitException, MemberNotFoundException {
        auth.requireCurrentUser();
        validateExpenseFields(description, amount);
        ensureMember(groupId, paidByUserId);

        List<User> members = groupDao.findMembers(groupId);
        List<Integer> participantIds = members.stream()
                .map(User::getUserId).collect(Collectors.toList());

        return saveExpense(groupId, description, amount, paidByUserId,
                category, SplitType.EQUAL, expenseDate, notes,
                new EqualSplitStrategy(), participantIds);
    }

    /**
     * Adds an expense split equally among a specified subset of members.
     */
    public Expense addExpense(int groupId, String description, BigDecimal amount,
                              int paidByUserId, ExpenseCategory category,
                              LocalDate expenseDate, String notes,
                              List<Integer> participantIds)
            throws AuthenticationException, DatabaseException,
                   InvalidExpenseException, InvalidSplitException, MemberNotFoundException {
        auth.requireCurrentUser();
        validateExpenseFields(description, amount);
        ensureMember(groupId, paidByUserId);
        for (int uid : participantIds) ensureMember(groupId, uid);

        return saveExpense(groupId, description, amount, paidByUserId,
                category, SplitType.EQUAL, expenseDate, notes,
                new EqualSplitStrategy(), participantIds);
    }

    /**
     * Adds an expense with custom per-member amounts.
     * @param amountsByUserId must include every participant and sum to {@code amount}
     */
    public Expense addExpenseCustomSplit(int groupId, String description, BigDecimal amount,
                                         int paidByUserId, ExpenseCategory category,
                                         LocalDate expenseDate, String notes,
                                         Map<Integer, BigDecimal> amountsByUserId)
            throws AuthenticationException, DatabaseException,
                   InvalidExpenseException, InvalidSplitException, MemberNotFoundException {
        auth.requireCurrentUser();
        validateExpenseFields(description, amount);
        ensureMember(groupId, paidByUserId);
        for (int uid : amountsByUserId.keySet()) ensureMember(groupId, uid);

        List<Integer> participantIds = List.copyOf(amountsByUserId.keySet());
        return saveExpense(groupId, description, amount, paidByUserId,
                category, SplitType.CUSTOM, expenseDate, notes,
                new CustomSplitStrategy(amountsByUserId), participantIds);
    }

    // ── Read ──────────────────────────────────────────────────────────────

    public List<Expense> getGroupExpenses(int groupId)
            throws AuthenticationException, DatabaseException {
        auth.requireCurrentUser();
        return expenseDao.findByGroupId(groupId);
    }

    // ── Delete ────────────────────────────────────────────────────────────

    /**
     * Deletes an expense. Only the user who recorded it (paidBy) or the group creator may delete it.
     */
    public void deleteExpense(int expenseId)
            throws AuthenticationException, DatabaseException, MemberNotFoundException {
        User user = auth.requireCurrentUser();
        Expense expense = expenseDao.findById(expenseId)
                .orElseThrow(() -> new MemberNotFoundException("Expense not found."));
        boolean isPayer   = expense.getPaidBy() == user.getUserId();
        boolean isCreator = groupDao.findById(expense.getGroupId())
                .map(g -> g.getCreatedBy() == user.getUserId())
                .orElse(false);
        if (!isPayer && !isCreator) {
            throw new AuthenticationException(
                    "Only the person who paid or the group creator can delete this expense.");
        }
        expenseDao.delete(expenseId);
    }

    // ── Balances and settlements ───────────────────────────────────────────

    /** Returns net balance per member (positive = owed money, negative = owes money). */
    public Map<Integer, BigDecimal> getBalances(int groupId)
            throws AuthenticationException, DatabaseException {
        auth.requireCurrentUser();
        List<User>    members  = groupDao.findMembers(groupId);
        List<Expense> expenses = expenseDao.findByGroupId(groupId);
        List<Integer> memberIds = members.stream()
                .map(User::getUserId).collect(Collectors.toList());
        return settlement.calculateBalances(memberIds, expenses);
    }

    /** Returns the minimal list of payments that settles the group. */
    public List<Settlement> getSettlements(int groupId)
            throws AuthenticationException, DatabaseException {
        return settlement.calculateSettlements(getBalances(groupId));
    }

    // ── Internals ─────────────────────────────────────────────────────────

    private Expense saveExpense(int groupId, String description, BigDecimal amount,
                                int paidByUserId, ExpenseCategory category,
                                SplitType splitType, LocalDate expenseDate, String notes,
                                SplitStrategy strategy, List<Integer> participantIds)
            throws DatabaseException, InvalidSplitException {
        List<ExpenseSplit> splits = strategy.calculateSplits(amount, participantIds);

        Expense expense = new Expense(groupId, description, Money.normalize(amount),
                paidByUserId, category, splitType, expenseDate, notes);
        for (ExpenseSplit s : splits) expense.addSplit(s);
        expenseDao.insert(expense);
        return expense;
    }

    private void validateExpenseFields(String description, BigDecimal amount)
            throws InvalidExpenseException {
        if (description == null || description.isBlank())
            throw new InvalidExpenseException("Description is required.");
        if (description.trim().length() > 255)
            throw new InvalidExpenseException("Description must be 255 characters or fewer.");
        if (amount == null)
            throw new InvalidExpenseException("Amount is required.");
        if (amount.signum() <= 0)
            throw new InvalidExpenseException("Amount must be greater than zero.");
        if (!Money.hasValidScale(amount))
            throw new InvalidExpenseException("Amount can have at most 2 decimal places.");
    }

    private void ensureMember(int groupId, int userId)
            throws DatabaseException, MemberNotFoundException {
        if (!groupDao.isMember(groupId, userId)) {
            throw new MemberNotFoundException("User " + userId + " is not a member of this group.");
        }
    }
}
