package com.smartsplit.service;

import com.smartsplit.dao.UserDAO;
import com.smartsplit.exception.AuthenticationException;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.model.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Handles user registration and login.
 * Passwords are hashed with PBKDF2-HMAC-SHA256 (256-bit salt, 100 000 iterations, 256-bit key).
 * The stored hash format is: iterations$base64(salt)$base64(hash)
 * No external libraries — only java.security and javax.crypto (standard JDK).
 */
public class AuthenticationService {

    private static final String ALGORITHM     = "PBKDF2WithHmacSHA256";
    private static final int    ITERATIONS    = 100_000;
    private static final int    KEY_LENGTH    = 256;   // bits
    private static final int    SALT_LENGTH   = 32;    // bytes

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)*\\.[a-zA-Z]{2,}$");

    private final UserDAO userDao;
    private User currentUser = null;

    public AuthenticationService(UserDAO userDao) {
        this.userDao = Objects.requireNonNull(userDao);
    }

    // ── Registration ──────────────────────────────────────────────────────

    /**
     * Registers a new user.
     * @throws AuthenticationException if name/email/password fail validation or the email is taken
     * @throws DatabaseException       on database error
     */
    public User register(String name, String email, String password)
            throws AuthenticationException, DatabaseException {
        validateName(name);
        validateEmail(email);
        validatePassword(password);

        String normalizedEmail = email.trim().toLowerCase();
        if (userDao.findByEmail(normalizedEmail).isPresent()) {
            throw new AuthenticationException("An account with this email already exists.");
        }

        String hash = hashPassword(password);
        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(hash);
        userDao.insert(user);
        return user;
    }

    // ── Login / Logout ────────────────────────────────────────────────────

    /**
     * Logs in a user by email and password.
     * @throws AuthenticationException if credentials are wrong
     * @throws DatabaseException       on database error
     */
    public User login(String email, String password)
            throws AuthenticationException, DatabaseException {
        if (email == null || email.isBlank())
            throw new AuthenticationException("Email is required.");
        if (password == null || password.isEmpty())
            throw new AuthenticationException("Password is required.");

        Optional<User> found = userDao.findByEmail(email.trim().toLowerCase());
        if (found.isEmpty() || !verifyPassword(password, found.get().getPasswordHash())) {
            // Same message for both to prevent user enumeration
            throw new AuthenticationException("Incorrect email or password.");
        }

        currentUser = found.get();
        return currentUser;
    }

    public void logout() { currentUser = null; }

    /** Returns the currently logged-in user, or empty if nobody is logged in. */
    public Optional<User> getCurrentUser() { return Optional.ofNullable(currentUser); }

    /** Throws if no user is logged in — convenience for other services. */
    public User requireCurrentUser() throws AuthenticationException {
        if (currentUser == null) throw new AuthenticationException("You must be logged in.");
        return currentUser;
    }

    // ── Validation ────────────────────────────────────────────────────────

    private void validateName(String name) throws AuthenticationException {
        if (name == null || name.isBlank())
            throw new AuthenticationException("Name is required.");
        if (name.trim().length() > 100)
            throw new AuthenticationException("Name must be 100 characters or fewer.");
    }

    private void validateEmail(String email) throws AuthenticationException {
        if (email == null || email.isBlank())
            throw new AuthenticationException("Email is required.");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches())
            throw new AuthenticationException("Please enter a valid email address.");
        if (email.trim().length() > 255)
            throw new AuthenticationException("Email must be 255 characters or fewer.");
    }

    private void validatePassword(String password) throws AuthenticationException {
        if (password == null || password.isEmpty())
            throw new AuthenticationException("Password is required.");
        if (password.length() < 8)
            throw new AuthenticationException("Password must be at least 8 characters.");
        if (password.length() > 128)
            throw new AuthenticationException("Password must be 128 characters or fewer.");
    }

    // ── Password hashing (PBKDF2-HMAC-SHA256) ─────────────────────────────

    String hashPassword(String password) {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);
            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            return ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing is not available on this JVM.", e);
        }
    }

    boolean verifyPassword(String password, String storedHash) {
        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 3) return false;
            int    iterations = Integer.parseInt(parts[0]);
            byte[] salt       = Base64.getDecoder().decode(parts[1]);
            byte[] expected   = Base64.getDecoder().decode(parts[2]);
            byte[] actual     = pbkdf2(password.toCharArray(), salt, iterations, expected.length * 8);
            return constantTimeEquals(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    /** Constant-time comparison to prevent timing attacks. */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }
}
