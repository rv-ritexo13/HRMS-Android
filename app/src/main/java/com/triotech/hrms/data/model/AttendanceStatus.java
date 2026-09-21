package com.triotech.hrms.data.model;

/**
 * Categorical attendance status shown in the History list, the Calendar and
 * the Monthly Summary — distinct from {@link AttendanceRecord.Status}, which
 * only models *today's* live check-in/check-out button state.
 */
public enum AttendanceStatus {
    PRESENT,
    LATE,
    HALF_DAY,
    ABSENT,
    LEAVE,
    WORK_FROM_HOME
}
