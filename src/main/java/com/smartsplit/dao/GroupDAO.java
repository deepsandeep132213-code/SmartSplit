package com.smartsplit.dao;

import com.smartsplit.database.DBConnection;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.model.Group;
import com.smartsplit.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** DAO for groups and membership. {@code `groups`} is always backtick-quoted (reserved word in MySQL 8). */
public class GroupDAO {

    private static final String SEL = "SELECT g.group_id, g.group_name, g.created_by, g.is_archived, g.created_at FROM `groups` g ";
    private final DBConnection db;

    public GroupDAO(DBConnection db) { this.db = Objects.requireNonNull(db); }

    public void insert(Group group) throws DatabaseException {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                int gid = insertGroup(c, group); group.setGroupId(gid);
                insertMember(c, gid, group.getCreatedBy());
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        } catch (SQLException e) { throw new DatabaseException("Could not create the group.", e); }
    }

    private int insertGroup(Connection c, Group g) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO `groups` (group_name, created_by) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.getGroupName()); ps.setInt(2, g.getCreatedBy()); ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (!k.next()) throw new SQLException("No group id generated."); return k.getInt(1); }
        }
    }

    private boolean insertMember(Connection c, int gid, int uid) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO group_members (group_id, user_id) VALUES (?, ?)")) {
            ps.setInt(1, gid); ps.setInt(2, uid); return ps.executeUpdate() > 0;
        }
    }

    public Optional<Group> findById(int groupId) throws DatabaseException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(SEL + "WHERE g.group_id = ?")) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapGroup(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new DatabaseException("Could not load the group.", e); }
    }

    public List<Group> findByUserId(int userId) throws DatabaseException {
        String sql = SEL + "JOIN group_members gm ON gm.group_id = g.group_id "
                + "WHERE gm.user_id = ? AND g.is_archived = FALSE ORDER BY g.created_at DESC, g.group_id DESC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Group> list = new ArrayList<>();
                while (rs.next()) list.add(mapGroup(rs));
                return list;
            }
        } catch (SQLException e) { throw new DatabaseException("Could not load your groups.", e); }
    }

    public boolean setArchived(int groupId, boolean archived) throws DatabaseException {
        return update("UPDATE `groups` SET is_archived = ? WHERE group_id = ?",
                ps -> { ps.setBoolean(1, archived); ps.setInt(2, groupId); }, "Could not update the group.");
    }

    public boolean delete(int groupId) throws DatabaseException {
        return update("DELETE FROM `groups` WHERE group_id = ?", ps -> ps.setInt(1, groupId), "Could not delete the group.");
    }

    public boolean addMember(int groupId, int userId) throws DatabaseException {
        try (Connection c = db.getConnection()) { return insertMember(c, groupId, userId); }
        catch (SQLException e) { throw new DatabaseException("Could not add the member.", e); }
    }

    public boolean removeMember(int groupId, int userId) throws DatabaseException {
        return update("DELETE FROM group_members WHERE group_id = ? AND user_id = ?",
                ps -> { ps.setInt(1, groupId); ps.setInt(2, userId); }, "Could not remove the member.");
    }

    public boolean isMember(int groupId, int userId) throws DatabaseException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM group_members WHERE group_id = ? AND user_id = ?")) {
            ps.setInt(1, groupId); ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new DatabaseException("Could not check membership.", e); }
    }

    public List<User> findMembers(int groupId) throws DatabaseException {
        String sql = "SELECT u.user_id, u.name, u.email, u.password_hash, u.created_at FROM users u "
                + "JOIN group_members gm ON gm.user_id = u.user_id WHERE gm.group_id = ? ORDER BY gm.joined_at, u.user_id";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> members = new ArrayList<>();
                while (rs.next()) members.add(UserDAO.mapRow(rs));
                return members;
            }
        } catch (SQLException e) { throw new DatabaseException("Could not load group members.", e); }
    }

    @FunctionalInterface private interface ParamSetter { void set(PreparedStatement ps) throws SQLException; }

    private boolean update(String sql, ParamSetter s, String err) throws DatabaseException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            s.set(ps); return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new DatabaseException(err, e); }
    }

    private static Group mapGroup(ResultSet rs) throws SQLException {
        return new Group(rs.getInt("group_id"), rs.getString("group_name"), rs.getInt("created_by"),
                rs.getBoolean("is_archived"), rs.getTimestamp("created_at").toLocalDateTime());
    }
}
