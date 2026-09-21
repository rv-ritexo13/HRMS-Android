package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Today's attendance state for the signed-in employee, shown on the Home
 * dashboard's Attendance card. GPS/geofenced check-in, historical timesheets
 * and shift management are out of scope for Phase 2 — this is a same-day,
 * in-memory mock (see {@link com.triotech.hrms.data.repository.FakeAttendanceRepository}).
 */
public class AttendanceRecord {

    public enum Status {
        NOT_CHECKED_IN,
        CHECKED_IN,
        CHECKED_OUT
    }

    @NonNull private final Status status;
    @Nullable private final Long checkInTimeMillis;
    @Nullable private final Long checkOutTimeMillis;

    public AttendanceRecord(
            @NonNull Status status, @Nullable Long checkInTimeMillis, @Nullable Long checkOutTimeMillis) {
        this.status = status;
        this.checkInTimeMillis = checkInTimeMillis;
        this.checkOutTimeMillis = checkOutTimeMillis;
    }

    @NonNull
    public Status getStatus() {
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

    public boolean canCheckIn() {
        return status == Status.NOT_CHECKED_IN;
    }

    public boolean canCheckOut() {
        return status == Status.CHECKED_IN;
    }

    /** Working duration in milliseconds, only meaningful once checked out. */
    public long getWorkingDurationMillis() {
        if (checkInTimeMillis == null || checkOutTimeMillis == null) {
            return 0L;
        }
        return Math.max(0L, checkOutTimeMillis - checkInTimeMillis);
    }
}
