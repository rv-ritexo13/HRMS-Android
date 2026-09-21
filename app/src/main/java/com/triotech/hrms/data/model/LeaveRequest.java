package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A single leave application and its lifecycle. Persisted in the
 * {@code leave_requests} table of {@link com.triotech.hrms.data.local.HrmsDatabase}.
 */
public final class LeaveRequest {

    private final String id;
    private final LeaveType type;
    private final long startMillis;
    private final long endMillis;
    private final int days;
    private final String reason;
    private final LeaveStatus status;
    private final long appliedMillis;
    private final String managerComments;
    @Nullable private final String attachmentName;

    public LeaveRequest(
            @NonNull String id,
            @NonNull LeaveType type,
            long startMillis,
            long endMillis,
            int days,
            @NonNull String reason,
            @NonNull LeaveStatus status,
            long appliedMillis,
            @NonNull String managerComments,
            @Nullable String attachmentName) {
        this.id = id;
        this.type = type;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
        this.days = days;
        this.reason = reason;
        this.status = status;
        this.appliedMillis = appliedMillis;
        this.managerComments = managerComments;
        this.attachmentName = attachmentName;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public LeaveType getType() {
        return type;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public long getEndMillis() {
        return endMillis;
    }

    public int getDays() {
        return days;
    }

    @NonNull
    public String getReason() {
        return reason;
    }

    @NonNull
    public LeaveStatus getStatus() {
        return status;
    }

    public long getAppliedMillis() {
        return appliedMillis;
    }

    @NonNull
    public String getManagerComments() {
        return managerComments;
    }

    @Nullable
    public String getAttachmentName() {
        return attachmentName;
    }

    public boolean hasManagerComments() {
        return !managerComments.trim().isEmpty();
    }
}
