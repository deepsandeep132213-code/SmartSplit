package com.smartsplit.service;

import com.smartsplit.dao.UserDAO;
import com.smartsplit.database.DBConnection;
import com.smartsplit.exception.AuthenticationException;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.model.User;
import com.smartsplit.testrunner.Assertions;
import com.smartsplit.testrunner.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tests AuthenticationService logic without a real database.
 * Uses an in-memory stub UserDAO.
 */
public class AuthenticationServiceTest {

    // ── In-memory stub ────────────────────────────────────────────────────

    static class StubUserDAO extends UserDAO {
        final Map<String, User> byEmail = new HashMap<>();
        int nextId = 1;

        StubUserDAO() { super(); }  // no DBConnection — we override all methods

        @Override public void insert(User user) {
            user.setUserId(nextId++);
            byEmail.put(user.getEmail(), user);
        }

        @Override public Optional<User> findByEmail(String email) {
            return Optional.ofNullable(byEmail.get(email));
        }

        @Override public Optional<User> findById(int id) {
            return byEmail.values().stream().filter(u -> u.getUserId() == id).findFirst();
        }
    }

    // ── Setup helper ──────────────────────────────────────────────────────

    private StubUserDAO dao;
    private AuthenticationService auth;

    void setUp() {
        dao  = new StubUserDAO();
        auth = new AuthenticationService(dao);
    }

    // ── Password hashing ─────────────────────────────────────────────────

    @Test public void hashAndVerify_roundtrip() {
        setUp();
        String hash = auth.hashPassword("MyP@ssw0rd!");
        Assertions.assertTrue(auth.verifyPassword("MyP@ssw0rd!", hash));
    }

    @Test public void verifyPassword_wrongPassword_returnsFalse() {
        setUp();
        String hash = auth.hashPassword("correct");
        Assertions.assertFalse(auth.verifyPassword("wrong", hash));
    }

    @Test public void hashPassword_twoCallsSameInput_differentHashes() {
        setUp();
        // Salting: same password must produce different stored hashes
        String h1 = auth.hashPassword("same");
        String h2 = auth.hashPassword("same");
        Assertions.assertFalse(h1.equals(h2), "Same password must produce different hashes (salted)");
    }

    @Test public void hashPassword_formatContainsThreeParts() {
        setUp();
        String hash = auth.hashPassword("test");
        String[] parts = hash.split("\\$");
        Assertions.assertEquals(3, parts.length);
    }

    // ── Register ──────────────────────────────────────────────────────────

    @Test public void register_validUser_savesToDao() throws Exception {
        setUp();
        User u = auth.register("Alice", "alice@example.com", "password1");
        Assertions.assertNotNull(u);
        Assertions.assertEquals("alice@example.com", u.getEmail());
        Assertions.assertEquals("Alice", u.getName());
        Assertions.assertTrue(dao.byEmail.containsKey("alice@example.com"));
    }

    @Test public void register_duplicateEmail_throwsAuthException() throws Exception {
        setUp();
        auth.register("Alice", "alice@example.com", "password1");
        Assertions.assertThrows(AuthenticationException.class,
                () -> auth.register("Alice2", "alice@example.com", "password2"));
    }

    @Test public void register_emailNormalised_lowercase() throws Exception {
        setUp();
        auth.register("Bob", "BOB@EXAMPLE.COM", "password1");
        Assertions.assertTrue(dao.byEmail.containsKey("bob@example.com"));
    }

    @Test public void register_blankName_throwsAuthException() {
        setUp();
        Assertions.assertThrows(AuthenticationException.class,
                () -> auth.register("  ", "a@b.com", "password1"));
    }

    @Test public void register_invalidEmail_throwsAuthException() {
        setUp();
        Assertions.assertThrows(AuthenticationException.class,
                () -> auth.register("Name", "not-an-email", "password1"));
    }

    @Test public void register_shortPassword_throwsAuthException() {
        setUp();
        Assertions.assertThrows(AuthenticationException.class,
                () -> auth.register("Name", "a@b.com", "short"));
    }

    @Test public void register_passwordNotStoredAsPlaintext() throws Exception {
        setUp();
        auth.register("Charlie", "c@example.com", "mypassword");
        User saved = dao.byEmail.get("c@example.com");
        Assertions.assertFalse(saved.getPasswordHash().contains("mypassword"),
                "Password must not be stored as plain text");
    }

    // ── Login ─────────────────────────────────────────────────────────────

    @Test public void login_validCredentials_returnsUser() throws Exception {
        setUp();
        auth.register("Dana", "dana@example.com", "goodpassword");
        User u = auth.login("dana@example.com", "goodpassword");
        Assertions.assertNotNull(u);
        Assertions.assertEquals("dana@example.com", u.getEmail());
    }

    @Test public void login_wrongPassword_throwsAuthException() throws Exception {
        setUp();
        auth.register("Eve", "eve@example.com", "correct1");
        Assertions.assertThrows(AuthenticationException.class,
                () -> auth.login("eve@example.com", "wrong"));
    }

    @Test public void login_unknownEmail_throwsAuthException() {
        setUp();
        Assertions.assertThrows(AuthenticationException.class,
                () -> auth.login("nobody@example.com", "pass"));
    }

    @Test public void login_setsCurrentUser() throws Exception {
        setUp();
        auth.register("Frank", "frank@example.com", "password1");
        Assertions.assertTrue(auth.getCurrentUser().isEmpty());
        auth.login("frank@example.com", "password1");
        Assertions.assertTrue(auth.getCurrentUser().isPresent());
        Assertions.assertEquals("frank@example.com", auth.getCurrentUser().get().getEmail());
    }

    @Test public void logout_clearsCurrentUser() throws Exception {
        setUp();
        auth.register("Gail", "gail@example.com", "password1");
        auth.login("gail@example.com", "password1");
        auth.logout();
        Assertions.assertTrue(auth.getCurrentUser().isEmpty());
    }

    @Test public void requireCurrentUser_notLoggedIn_throwsAuthException() {
        setUp();
        Assertions.assertThrows(AuthenticationException.class,
                () -> auth.requireCurrentUser());
    }
}
