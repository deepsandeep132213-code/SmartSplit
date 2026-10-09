package com.smartsplit.model;

public enum ExpenseCategory {
    FOOD("Food"), TRAVEL("Travel"), HOTEL("Hotel"),
    SHOPPING("Shopping"), ENTERTAINMENT("Entertainment"), OTHER("Other");

    private final String displayName;
    ExpenseCategory(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
