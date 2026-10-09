package com.smartsplit.dao;

import com.smartsplit.database.DBConnection;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.model.ExpenseSplit;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** DAO for expense_splits rows. Usually called inside the same transaction as ExpenseDAO. */
public class ExpenseSplitDAO {

    private final DBConnection db;

    public ExpenseSplitDAO(DBConnection db) { this.db = Objects.requireNonNull(db); }

    /** Inserts a single split and sets its generated split_id on the object. */
    void insert(Connection c, ExpenseSplit split) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO expense_splits (expense_id, user_id, amount) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, split.getExpenseId());
            ps.setInt(2, split.getUserId());
            ps.setBigDecimal(3, split.getAmount());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) split.setSplitId(k.getInt(1));
            }
        }
    }

    /** Inserts all splits for an expense within an existing transaction. */
    void insertAll(Connection c, List<ExpenseSplit> splits) throws SQLException {
        for (ExpenseSplit s : splits) insert(c, s);
    }

    public List<ExpenseSplit> findByExpenseId(int expenseId) throws DatabaseException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT split_id, expense_id, user_id, amount FROM expense_splits WHERE expense_id = ? ORDER BY split_id")) {
            ps.setInt(1, expenseId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ExpenseSplit> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) { throw new DatabaseException("Could not load expense splits.", e); }
    }

    /** Deletes all splits for an expense. Called inside the expense-deletion transaction. */
    void deleteByExpenseId(Connection c, int expenseId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM expense_splits WHERE expense_id = ?")) {
            ps.setInt(1, expenseId);
            ps.executeUpdate();
        }
    }

    private static ExpenseSplit mapRow(ResultSet rs) throws SQLException {
        ExpenseSplit s = new ExpenseSplit(rs.getInt("user_id"), rs.getBigDecimal("amount"));
        s.setSplitId(rs.getInt("split_id"));
        s.setExpenseId(rs.getInt("expense_id"));
        return s;
    }
}
