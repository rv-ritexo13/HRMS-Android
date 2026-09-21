package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Lifecycle status of an {@link Expense}. Pure enum; the UI maps each value to a
 * coloured status chip via {@code ExpensePresenter}. Step 1 only creates DRAFT
 * and SUBMITTED expenses locally — APPROVED / REJECTED / PAID exist so seeded
 * demo data and later approval phases have somewhere to land.
 */
public enum ExpenseStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    PAID;

    @NonNull
    public String key() {
        return name();
    }

    @Nullable
    public static ExpenseStatus fromKey(@Nullable String key) {
        if (key == null) {
            return null;
        }
        for (ExpenseStatus status : values()) {
            if (status.name().equals(key)) {
                return status;
            }
        }
        return null;
    }
}
