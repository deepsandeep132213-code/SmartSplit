package com.smartsplit.ai;

/** Checked exception for AI/LLM and Places API errors. */
public class AIException extends Exception {
    public AIException(String message) { super(message); }
    public AIException(String message, Throwable cause) { super(message, cause); }
}
