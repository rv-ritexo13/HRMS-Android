package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;

/**
 * Core employee record. Kept intentionally small for Phase 1 — enough fields to
 * populate a realistic-looking directory list. Payroll, documents, org-chart and
 * other HRMS modules will extend this model in later phases.
 */
public class Employee {

    private final String id;
    private final String fullName;
    private final String designation;
    private final String department;
    private final EmploymentStatus status;
    private final String initials;

    public Employee(
            @NonNull String id,
            @NonNull String fullName,
            @NonNull String designation,
            @NonNull String department,
            @NonNull EmploymentStatus status) {
        this.id = id;
        this.fullName = fullName;
        this.designation = designation;
        this.department = department;
        this.status = status;
        this.initials = computeInitials(fullName);
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getFullName() {
        return fullName;
    }

    @NonNull
    public String getDesignation() {
        return designation;
    }

    @NonNull
    public String getDepartment() {
        return department;
    }

    @NonNull
    public EmploymentStatus getStatus() {
        return status;
    }

    @NonNull
    public String getInitials() {
        return initials;
    }

    private static String computeInitials(@NonNull String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        String[] parts = trimmed.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && i < 2; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return sb.length() > 0 ? sb.toString() : "?";
    }

    /** Employment status, used to drive the status chip color in the employee list. */
    public enum EmploymentStatus {
        ACTIVE,
        ON_LEAVE,
        NOTICE_PERIOD
    }
}
