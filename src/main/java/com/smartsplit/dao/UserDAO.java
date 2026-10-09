package com.smartsplit.dao;

import com.smartsplit.database.DBConnection;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.model.User;
import java.sql.*;
import java.util.Objects;
import java.util.Optional;

public class UserDAO {

    private static final String SELECT_COLS = "SELECT user_id, name, email, password_hash, created_at FROM users ";
    private final DBConnection db;

    public UserDAO(DBConnection db) { this.db = Objects.requireNonNull(db); }

    /** Protected constructor for test stubs that override all methods and don't use db. */
    protected UserDAO() { this.db = null; }

    public void insert(User user) throws DatabaseException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName()); ps.setString(2, user.getEmail()); ps.setString(3, user.getPasswordHash());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) user.setUserId(k.getInt(1)); }
        } catch (SQLException e) { throw new DatabaseException("Could not save the user.", e); }
    }

    public Optional<User> findById(int userId) throws DatabaseException {
        return findOne(SELECT_COLS + "WHERE user_id = ?", userId);
    }

    public Optional<User> findByEmail(String email) throws DatabaseException {
        return findOne(SELECT_COLS + "WHERE email = ?", email);
    }

    private Optional<User> findOne(String sql, Object param) throws DatabaseException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new DatabaseException("Could not load the user.", e); }
    }

    static User mapRow(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"),
                rs.getString("password_hash"), rs.getTimestamp("created_at").toLocalDateTime());
    }
}
