package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;

/**
 * The signed-in user's identity and profile summary, as returned by
 * {@link com.triotech.hrms.data.repository.AuthRepository} and persisted by
 * {@link com.triotech.hrms.core.util.SessionManager}.
 *
 * <p>Deliberately separate from {@link Employee} (the directory-listing model from
 * Phase 1): this represents "who is using the app right now", not a row in an
 * employee directory — the two happen to share shape today but model different
 * concepts and may diverge (e.g. an Admin account has no designation/department
 * in a real backend).</p>
 */
public class AuthUser {

    private final String employeeId;
    private final String fullName;
    private final String designation;
    private final String department;
    private final UserRole role;

    public AuthUser(
            @NonNull String employeeId,
            @NonNull String fullName,
            @NonNull String designation,
            @NonNull String department,
            @NonNull UserRole role) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.designation = designation;
        this.department = department;
        this.role = role;
    }

    @NonNull
    public String getEmployeeId() {
        return employeeId;
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
    public UserRole getRole() {
        return role;
    }

    @NonNull
    public String getInitials() {
        String trimmed = fullName.trim();
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
}
