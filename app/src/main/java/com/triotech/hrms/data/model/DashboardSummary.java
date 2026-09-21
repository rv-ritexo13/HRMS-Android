package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;

/**
 * The four "mini info card" values on the Home dashboard: Leave Balance,
 * Salary, Pending Tasks and Upcoming Meeting. Deliberately just display data —
 * actual leave/payroll/task logic is out of scope until those phases.
 */
public class DashboardSummary {

    private final int leaveBalanceDays;
    private final String salaryLabel;
    private final int pendingTasksCount;
    private final String upcomingMeetingTitle;
    private final String upcomingMeetingTimeLabel;

    public DashboardSummary(
            int leaveBalanceDays,
            @NonNull String salaryLabel,
            int pendingTasksCount,
            @NonNull String upcomingMeetingTitle,
            @NonNull String upcomingMeetingTimeLabel) {
        this.leaveBalanceDays = leaveBalanceDays;
        this.salaryLabel = salaryLabel;
        this.pendingTasksCount = pendingTasksCount;
        this.upcomingMeetingTitle = upcomingMeetingTitle;
        this.upcomingMeetingTimeLabel = upcomingMeetingTimeLabel;
    }

    public int getLeaveBalanceDays() {
        return leaveBalanceDays;
    }

    @NonNull
    public String getSalaryLabel() {
        return salaryLabel;
    }

    public int getPendingTasksCount() {
        return pendingTasksCount;
    }

    @NonNull
    public String getUpcomingMeetingTitle() {
        return upcomingMeetingTitle;
    }

    @NonNull
    public String getUpcomingMeetingTimeLabel() {
        return upcomingMeetingTimeLabel;
    }
}
