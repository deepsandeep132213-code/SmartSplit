package com.smartsplit.exception;

/** Thrown when login fails or a registration constraint is violated. */
public class AuthenticationException extends Exception {
    private static final long serialVersionUID = 1L;
    public AuthenticationException(String message) { super(message); }
}
