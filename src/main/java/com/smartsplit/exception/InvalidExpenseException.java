package com.smartsplit.exception;

/** Thrown when an expense fails validation (bad amount, empty description, etc.). */
public class InvalidExpenseException extends Exception {
    private static final long serialVersionUID = 1L;
    public InvalidExpenseException(String message) { super(message); }
}
