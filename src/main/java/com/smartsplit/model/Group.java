package com.smartsplit.model;

import java.time.LocalDateTime;

/** An expense-sharing group. Maps to the {@code groups} table. */
public class Group {
    private int groupId;
    private String groupName;
    private int createdBy;
    private boolean archived;
    private LocalDateTime createdAt;

    public Group() {}
    public Group(String groupName, int createdBy) { this.groupName = groupName; this.createdBy = createdBy; }
    public Group(int groupId, String groupName, int createdBy, boolean archived, LocalDateTime createdAt) {
        this(groupName, createdBy);
        this.groupId = groupId; this.archived = archived; this.createdAt = createdAt;
    }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    @Override public String toString() { return groupName; }
}
