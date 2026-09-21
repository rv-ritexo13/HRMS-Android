package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;

/**
 * The full employee profile shown on the Profile screen and edited via the Edit
 * Profile form. Persisted in the {@code profile} table of
 * {@link com.triotech.hrms.data.local.HrmsDatabase} (keyed by employee id) so edits
 * survive app restarts and are inspectable.
 *
 * <p>Immutable: {@link #withEdits} returns a copy with the user-editable fields
 * changed while HR-controlled fields (id, department, designation, manager,
 * joining date, employment type) are preserved.</p>
 */
public final class EmployeeProfile {

    // Identity / HR-controlled (read-only in the edit form)
    private final String employeeId;
    private final String department;
    private final String designation;
    private final String reportingManager;
    private final long joiningDateMillis;
    private final String employmentType;

    // User-editable
    private final String fullName;
    private final long dateOfBirthMillis;
    private final String gender;
    private final String phoneNumber;
    private final String workEmail;
    private final String officeLocation;
    private final String emergencyName;
    private final String emergencyRelationship;
    private final String emergencyPhone;

    public EmployeeProfile(
            @NonNull String employeeId,
            @NonNull String department,
            @NonNull String designation,
            @NonNull String reportingManager,
            long joiningDateMillis,
            @NonNull String employmentType,
            @NonNull String fullName,
            long dateOfBirthMillis,
            @NonNull String gender,
            @NonNull String phoneNumber,
            @NonNull String workEmail,
            @NonNull String officeLocation,
            @NonNull String emergencyName,
            @NonNull String emergencyRelationship,
            @NonNull String emergencyPhone) {
        this.employeeId = employeeId;
        this.department = department;
        this.designation = designation;
        this.reportingManager = reportingManager;
        this.joiningDateMillis = joiningDateMillis;
        this.employmentType = employmentType;
        this.fullName = fullName;
        this.dateOfBirthMillis = dateOfBirthMillis;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.workEmail = workEmail;
        this.officeLocation = officeLocation;
        this.emergencyName = emergencyName;
        this.emergencyRelationship = emergencyRelationship;
        this.emergencyPhone = emergencyPhone;
    }

    @NonNull
    public EmployeeProfile withEdits(
            @NonNull String fullName,
            long dateOfBirthMillis,
            @NonNull String gender,
            @NonNull String phoneNumber,
            @NonNull String workEmail,
            @NonNull String officeLocation,
            @NonNull String emergencyName,
            @NonNull String emergencyRelationship,
            @NonNull String emergencyPhone) {
        return new EmployeeProfile(
                employeeId, department, designation, reportingManager, joiningDateMillis, employmentType,
                fullName, dateOfBirthMillis, gender, phoneNumber, workEmail, officeLocation,
                emergencyName, emergencyRelationship, emergencyPhone);
    }

    @NonNull
    public String getEmployeeId() {
        return employeeId;
    }

    @NonNull
    public String getDepartment() {
        return department;
    }

    @NonNull
    public String getDesignation() {
        return designation;
    }

    @NonNull
    public String getReportingManager() {
        return reportingManager;
    }

    public long getJoiningDateMillis() {
        return joiningDateMillis;
    }

    @NonNull
    public String getEmploymentType() {
        return employmentType;
    }

    @NonNull
    public String getFullName() {
        return fullName;
    }

    public long getDateOfBirthMillis() {
        return dateOfBirthMillis;
    }

    @NonNull
    public String getGender() {
        return gender;
    }

    @NonNull
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @NonNull
    public String getWorkEmail() {
        return workEmail;
    }

    @NonNull
    public String getOfficeLocation() {
        return officeLocation;
    }

    @NonNull
    public String getEmergencyName() {
        return emergencyName;
    }

    @NonNull
    public String getEmergencyRelationship() {
        return emergencyRelationship;
    }

    @NonNull
    public String getEmergencyPhone() {
        return emergencyPhone;
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
