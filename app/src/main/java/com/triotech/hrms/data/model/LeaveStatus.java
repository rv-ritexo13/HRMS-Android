package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Lifecycle status of a {@link LeaveRequest}. Pure enum; the UI maps each value to
 * a coloured status chip via {@code LeavePresenter}.
 */
public enum LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    @NonNull
    public String key() {
        return name();
    }

    @Nullable
    public static LeaveStatus fromKey(@Nullable String key) {
        if (key == null) {
            return null;
        }
        for (LeaveStatus status : values()) {
            if (status.name().equals(key)) {
                return status;
            }
        }
        return null;
    }
}
