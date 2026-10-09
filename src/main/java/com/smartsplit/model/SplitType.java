package com.smartsplit.model;

public enum SplitType {
    EQUAL("Equal"), CUSTOM("Custom");
    private final String displayName;
    SplitType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
