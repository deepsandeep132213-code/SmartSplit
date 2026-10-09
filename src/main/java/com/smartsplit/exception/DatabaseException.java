package com.smartsplit.exception;

/** Thrown when a database operation fails. Message is safe to show to users. */
public class DatabaseException extends Exception {
    private static final long serialVersionUID = 1L;
    public DatabaseException(String message) { super(message); }
    public DatabaseException(String message, Throwable cause) { super(message, cause); }
}
