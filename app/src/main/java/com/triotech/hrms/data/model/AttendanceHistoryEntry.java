package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A single past day's attendance, shown in the History list and the Calendar.
 * Mock/deterministic for Phase 3 (see {@code FakeAttendanceRepository}); a real
 * backend would return these same fields from a timesheet endpoint, so nothing
 * in the UI layer needs to change when one is wired up.
 */
public class AttendanceHistoryEntry {

    @NonNull private final String dateKey;
    private final long dateMillis;
    @NonNull private final AttendanceStatus status;
    @Nullable private final Long checkInTimeMillis;
    @Nullable private final Long checkOutTimeMillis;

    public AttendanceHistoryEntry(
            @NonNull String dateKey,
            long dateMillis,
            @NonNull AttendanceStatus status,
            @Nullable Long checkInTimeMillis,
            @Nullable Long checkOutTimeMillis) {
        this.dateKey = dateKey;
        this.dateMillis = dateMillis;
        this.status = status;
        this.checkInTimeMillis = checkInTimeMillis;
        this.checkOutTimeMillis = checkOutTimeMillis;
    }

    @NonNull
    public String getDateKey() {
        return dateKey;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    @NonNull
    public AttendanceStatus getStatus() {
        return status;
    }

    @Nullable
    public Long getCheckInTimeMillis() {
        return checkInTimeMillis;
    }

    @Nullable
    public Long getCheckOutTimeMillis() {
        return checkOutTimeMillis;
    }

    /** A check-in was recorded but no check-out followed — the "missing check-out" edge case. */
    public boolean isMissingCheckOut() {
        return checkInTimeMillis != null && checkOutTimeMillis == null;
    }

    public long getWorkingDurationMillis() {
        if (checkInTimeMillis == null || checkOutTimeMillis == null) {
            return 0L;
        }
        return Math.max(0L, checkOutTimeMillis - checkInTimeMillis);
    }

    /** New copy with the check-in/out fields and status replaced — entries are otherwise immutable. */
    @NonNull
    public AttendanceHistoryEntry withTimes(
            @NonNull AttendanceStatus newStatus, @Nullable Long newCheckIn, @Nullable Long newCheckOut) {
        return new AttendanceHistoryEntry(dateKey, dateMillis, newStatus, newCheckIn, newCheckOut);
    }
}
