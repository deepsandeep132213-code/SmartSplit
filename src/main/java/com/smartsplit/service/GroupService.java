package com.smartsplit.service;

import com.smartsplit.dao.ExpenseDAO;
import com.smartsplit.dao.GroupDAO;
import com.smartsplit.dao.UserDAO;
import com.smartsplit.exception.AuthenticationException;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.exception.MemberNotFoundException;
import com.smartsplit.model.Expense;
import com.smartsplit.model.Group;
import com.smartsplit.model.User;

import java.util.List;
import java.util.Objects;

/**
 * Business logic for groups and membership.
 * All write operations require an authenticated user (via AuthenticationService).
 */
public class GroupService {

    private final GroupDAO              groupDao;
    private final UserDAO               userDao;
    private final ExpenseDAO            expenseDao;
    private final AuthenticationService auth;

    public GroupService(GroupDAO groupDao, UserDAO userDao,
                        ExpenseDAO expenseDao, AuthenticationService auth) {
        this.groupDao   = Objects.requireNonNull(groupDao);
        this.userDao    = Objects.requireNonNull(userDao);
        this.expenseDao = Objects.requireNonNull(expenseDao);
        this.auth       = Objects.requireNonNull(auth);
    }

    // ── Create ────────────────────────────────────────────────────────────

    /**
     * Creates a new group. The currently logged-in user becomes the creator and first member.
     * @throws IllegalArgumentException if the group name is blank or too long
     */
    public Group createGroup(String groupName)
            throws AuthenticationException, DatabaseException {
        User creator = auth.requireCurrentUser();
        validateGroupName(groupName);

        Group group = new Group();
        group.setGroupName(groupName.trim());
        group.setCreatedBy(creator.getUserId());
        groupDao.insert(group);
        return group;
    }

    // ── Read ──────────────────────────────────────────────────────────────

    /** Returns all active (non-archived) groups the current user belongs to, newest first. */
    public List<Group> getUserGroups() throws AuthenticationException, DatabaseException {
        User user = auth.requireCurrentUser();
        return groupDao.findByUserId(user.getUserId());
    }

    /**
     * Returns a group by id.
     * @throws MemberNotFoundException if the group does not exist
     */
    public Group getGroup(int groupId) throws DatabaseException, MemberNotFoundException {
        return groupDao.findById(groupId)
                .orElseThrow(() -> new MemberNotFoundException("Group not found."));
    }

    /** Returns all members of a group. */
    public List<User> getMembers(int groupId) throws DatabaseException {
        return groupDao.findMembers(groupId);
    }

    // ── Membership ────────────────────────────────────────────────────────

    /**
     * Adds a user (by email) to a group.
     * @throws MemberNotFoundException  if the user with that email does not exist
     * @throws IllegalStateException    if the user is already a member
     */
    public void addMember(int groupId, String email)
            throws AuthenticationException, DatabaseException, MemberNotFoundException {
        auth.requireCurrentUser();
        User user = userDao.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new MemberNotFoundException(
                        "No account found with email: " + email.trim()));
        if (groupDao.isMember(groupId, user.getUserId())) {
            throw new IllegalStateException(user.getName() + " is already a member of this group.");
        }
        groupDao.addMember(groupId, user.getUserId());
    }

    /**
     * Removes a user from a group.
     * Blocked if the user has paid for any expense in the group (to preserve ledger integrity).
     * @throws IllegalStateException if the user has unsettled expenses as payer
     */
    public void removeMember(int groupId, int userId)
            throws AuthenticationException, DatabaseException, MemberNotFoundException {
        auth.requireCurrentUser();
        if (!groupDao.isMember(groupId, userId)) {
            throw new MemberNotFoundException("User is not a member of this group.");
        }
        List<Expense> expenses = expenseDao.findByGroupId(groupId);
        boolean hasPaidExpense = expenses.stream()
                .anyMatch(e -> e.getPaidBy() == userId);
        if (hasPaidExpense) {
            throw new IllegalStateException(
                    "Cannot remove this member — they have paid for expenses in this group. " +
                    "Settle up first.");
        }
        groupDao.removeMember(groupId, userId);
    }

    // ── Archive / Delete ──────────────────────────────────────────────────

    public void archiveGroup(int groupId) throws AuthenticationException, DatabaseException {
        auth.requireCurrentUser();
        groupDao.setArchived(groupId, true);
    }

    public void unarchiveGroup(int groupId) throws AuthenticationException, DatabaseException {
        auth.requireCurrentUser();
        groupDao.setArchived(groupId, false);
    }

    /**
     * Permanently deletes a group and all its data (CASCADE in DB handles expenses/splits/members).
     * Only the creator may delete the group.
     */
    public void deleteGroup(int groupId)
            throws AuthenticationException, DatabaseException, MemberNotFoundException {
        User user  = auth.requireCurrentUser();
        Group group = getGroup(groupId);
        if (group.getCreatedBy() != user.getUserId()) {
            throw new AuthenticationException("Only the group creator can delete the group.");
        }
        groupDao.delete(groupId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void validateGroupName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Group name is required.");
        if (name.trim().length() > 100)
            throw new IllegalArgumentException("Group name must be 100 characters or fewer.");
    }
}
