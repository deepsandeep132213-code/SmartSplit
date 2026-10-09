package com.smartsplit.exception;

/** Thrown when split amounts are invalid (e.g., don't add up to the expense total). */
public class InvalidSplitException extends Exception {
    private static final long serialVersionUID = 1L;
    public InvalidSplitException(String message) { super(message); }
}
