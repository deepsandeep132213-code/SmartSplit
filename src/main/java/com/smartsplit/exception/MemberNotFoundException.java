package com.smartsplit.exception;

/** Thrown when an operation references a user who is not a member of the group. */
public class MemberNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;
    public MemberNotFoundException(String message) { super(message); }
}
