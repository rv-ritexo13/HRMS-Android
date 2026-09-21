package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The categories an expense can be filed under. Kept as a pure enum (no Android
 * references); the UI layer maps each value to a label, icon and accent colour
 * via {@code ExpensePresenter} — mirroring how {@link LeaveType} is handled.
 */
public enum ExpenseCategory {
    TRAVEL,
    FOOD,
    ACCOMMODATION,
    TRANSPORTATION,
    OFFICE,
    MEDICAL,
    OTHER;

    @NonNull
    public String key() {
        return name();
    }

    @Nullable
    public static ExpenseCategory fromKey(@Nullable String key) {
        if (key == null) {
            return null;
        }
        for (ExpenseCategory category : values()) {
            if (category.name().equals(key)) {
                return category;
            }
        }
        return null;
    }
}
