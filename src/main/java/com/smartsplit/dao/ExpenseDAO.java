package com.smartsplit.dao;

import com.smartsplit.database.DBConnection;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.model.Expense;
import com.smartsplit.model.ExpenseCategory;
import com.smartsplit.model.ExpenseSplit;
import com.smartsplit.model.SplitType;
import java.time.LocalDate;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** DAO for expenses and their splits. Saves/deletes both tables in one transaction. */
public class ExpenseDAO {

    private static final String SEL =
            "SELECT e.expense_id, e.group_id, e.description, e.amount, e.paid_by, " +
            "e.split_type, e.category, e.expense_date, e.notes, e.created_at FROM expenses e ";

    private final DBConnection db;
    private final ExpenseSplitDAO splitDao;

    public ExpenseDAO(DBConnection db) {
        this.db = Objects.requireNonNull(db);
        this.splitDao = new ExpenseSplitDAO(db);
    }

    /** Inserts the expense row and all its splits in one transaction. Sets generated ids. */
    public void insert(Expense expense) throws DatabaseException {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                int eid = insertExpenseRow(c, expense);
                expense.setExpenseId(eid);
                for (ExpenseSplit s : expense.getSplits()) s.setExpenseId(eid);
                splitDao.insertAll(c, expense.getSplits());
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        } catch (SQLException e) { throw new DatabaseException("Could not save the expense.", e); }
    }

    private int insertExpenseRow(Connection c, Expense e) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO expenses (group_id, description, amount, paid_by, split_type, category, expense_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, e.getGroupId());
            ps.setString(2, e.getDescription());
            ps.setBigDecimal(3, e.getAmount());
            ps.setInt(4, e.getPaidBy());
            ps.setString(5, e.getSplitType().name());
            ps.setString(6, e.getCategory().name());
            ps.setObject(7, e.getExpenseDate());   // java.sql driver handles LocalDate
            ps.setString(8, e.getNotes());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (!k.next()) throw new SQLException("No expense id generated.");
                return k.getInt(1);
            }
        }
    }

    public Optional<Expense> findById(int expenseId) throws DatabaseException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(SEL + "WHERE e.expense_id = ?")) {
            ps.setInt(1, expenseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Expense expense = mapRow(rs);
                expense.setSplits(splitDao.findByExpenseId(expenseId));
                return Optional.of(expense);
            }
        } catch (SQLException e) { throw new DatabaseException("Could not load the expense.", e); }
    }

    public List<Expense> findByGroupId(int groupId) throws DatabaseException {
        String sql = SEL + "WHERE e.group_id = ? ORDER BY e.created_at DESC, e.expense_id DESC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Expense> list = new ArrayList<>();
                while (rs.next()) {
                    Expense expense = mapRow(rs);
                    expense.setSplits(splitDao.findByExpenseId(expense.getExpenseId()));
                    list.add(expense);
                }
                return list;
            }
        } catch (SQLException e) { throw new DatabaseException("Could not load group expenses.", e); }
    }

    /** Deletes the expense row and all its splits in one transaction. */
    public boolean delete(int expenseId) throws DatabaseException {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                splitDao.deleteByExpenseId(c, expenseId);
                boolean deleted = deleteExpenseRow(c, expenseId);
                c.commit();
                return deleted;
            } catch (SQLException e) { c.rollback(); throw e; }
        } catch (SQLException e) { throw new DatabaseException("Could not delete the expense.", e); }
    }

    private boolean deleteExpenseRow(Connection c, int expenseId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM expenses WHERE expense_id = ?")) {
            ps.setInt(1, expenseId);
            return ps.executeUpdate() > 0;
        }
    }

    private static Expense mapRow(ResultSet rs) throws SQLException {
        Expense e = new Expense(
                rs.getInt("group_id"),
                rs.getString("description"),
                rs.getBigDecimal("amount"),
                rs.getInt("paid_by"),
                ExpenseCategory.valueOf(rs.getString("category")),
                SplitType.valueOf(rs.getString("split_type")),
                rs.getObject("expense_date", java.time.LocalDate.class),
                rs.getString("notes")
        );
        e.setExpenseId(rs.getInt("expense_id"));
        e.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return e;
    }
}
