package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;

/**
 * An employee's balance for one {@link LeaveType}: how many days are entitled for
 * the year and how many have been used, from which the available balance and a
 * progress ratio are derived for the balance cards.
 */
public final class LeaveBalance {

    private final LeaveType type;
    private final int totalDays;
    private final int usedDays;

    public LeaveBalance(@NonNull LeaveType type, int totalDays, int usedDays) {
        this.type = type;
        this.totalDays = totalDays;
        this.usedDays = usedDays;
    }

    @NonNull
    public LeaveType getType() {
        return type;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public int getUsedDays() {
        return usedDays;
    }

    public int getAvailableDays() {
        return Math.max(0, totalDays - usedDays);
    }

    /** 0-100, used out of total, for a progress indicator. */
    public int getUsedPercent() {
        if (totalDays <= 0) {
            return 0;
        }
        return Math.round(usedDays * 100f / totalDays);
    }
}
