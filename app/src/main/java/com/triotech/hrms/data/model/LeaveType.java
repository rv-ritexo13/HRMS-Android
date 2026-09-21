package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The kinds of leave an employee can hold a balance in and apply for. Kept as a
 * pure enum (no Android references); the UI layer maps each value to a label,
 * icon and accent colour via {@code LeavePresenter}.
 */
public enum LeaveType {
    CASUAL,
    SICK,
    EARNED,
    WORK_FROM_HOME,
    OPTIONAL_HOLIDAY;

    @NonNull
    public String key() {
        return name();
    }

    @Nullable
    public static LeaveType fromKey(@Nullable String key) {
        if (key == null) {
            return null;
        }
        for (LeaveType type : values()) {
            if (type.name().equals(key)) {
                return type;
            }
        }
        return null;
    }
}
