package com.triotech.hrms.data.model;

/** Aggregate day-counts by status for one calendar month, backing the Monthly Summary grid. */
public class MonthlyAttendanceSummary {

    private final int presentDays;
    private final int lateDays;
    private final int halfDays;
    private final int absentDays;
    private final int leaveDays;
    private final int workFromHomeDays;

    public MonthlyAttendanceSummary(
            int presentDays, int lateDays, int halfDays, int absentDays, int leaveDays, int workFromHomeDays) {
        this.presentDays = presentDays;
        this.lateDays = lateDays;
        this.halfDays = halfDays;
        this.absentDays = absentDays;
        this.leaveDays = leaveDays;
        this.workFromHomeDays = workFromHomeDays;
    }

    public int getPresentDays() {
        return presentDays;
    }

    public int getLateDays() {
        return lateDays;
    }

    public int getHalfDays() {
        return halfDays;
    }

    public int getAbsentDays() {
        return absentDays;
    }

    public int getLeaveDays() {
        return leaveDays;
    }

    public int getWorkFromHomeDays() {
        return workFromHomeDays;
    }
}
