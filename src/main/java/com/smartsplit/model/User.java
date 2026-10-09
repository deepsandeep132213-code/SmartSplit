package com.smartsplit.model;

import java.time.LocalDateTime;

/** A registered SmartSplit user. Maps to the {@code users} table. */
public class User {
    private int userId;
    private String name;
    private String email;
    private String passwordHash;
    private LocalDateTime createdAt;

    public User() {}

    public User(String name, String email, String passwordHash) {
        this.name = name; this.email = email; this.passwordHash = passwordHash;
    }

    public User(int userId, String name, String email, String passwordHash, LocalDateTime createdAt) {
        this(name, email, passwordHash);
        this.userId = userId; this.createdAt = createdAt;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Deliberately excludes the password hash to prevent leaking it in logs. */
    @Override
    public String toString() {
        return "User{id=" + userId + ", name='" + name + "', email='" + email + "'}";
    }
}
