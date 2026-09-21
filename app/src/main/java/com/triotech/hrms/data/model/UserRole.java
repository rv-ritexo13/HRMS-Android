package com.triotech.hrms.data.model;

/**
 * Distinguishes the two Phase 2 login destinations. {@link #EMPLOYEE} lands on the
 * full Employee Dashboard; {@link #ADMIN} lands on a placeholder Admin Dashboard
 * until admin-facing features are scoped in a later phase.
 */
public enum UserRole {
    EMPLOYEE,
    ADMIN
}
