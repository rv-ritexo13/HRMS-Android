package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An employee-submitted request to fix a wrong or missing check-in/check-out
 * time for a past day. Mock/in-memory for Phase 3, but structured so a real
 * {@code POST /attendance/corrections} endpoint can back this later without
 * any caller (ViewModel/Fragment) needing to change — see
 * {@link com.triotech.hrms.data.repository.AttendanceRepository}.
 */
public class AttendanceCorrectionRequest {

    @NonNull private final String id;
    @NonNull private final String dateKey;
    private final long dateMillis;
    @Nullable private final Long actualCheckInMillis;
    @Nullable private final Long expectedCheckInMillis;
    @NonNull private final String reason;
    @NonNull private final CorrectionStatus status;
    private final long submittedAtMillis;

    public AttendanceCorrectionRequest(
            @NonNull String id,
            @NonNull String dateKey,
            long dateMillis,
            @Nullable Long actualCheckInMillis,
            @Nullable Long expectedCheckInMillis,
            @NonNull String reason,
            @NonNull CorrectionStatus status,
            long submittedAtMillis) {
        this.id = id;
        this.dateKey = dateKey;
        this.dateMillis = dateMillis;
        this.actualCheckInMillis = actualCheckInMillis;
        this.expectedCheckInMillis = expectedCheckInMillis;
        this.reason = reason;
        this.status = status;
        this.submittedAtMillis = submittedAtMillis;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getDateKey() {
        return dateKey;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    @Nullable
    public Long getActualCheckInMillis() {
        return actualCheckInMillis;
    }

    @Nullable
    public Long getExpectedCheckInMillis() {
        return expectedCheckInMillis;
    }

    @NonNull
    public String getReason() {
        return reason;
    }

    @NonNull
    public CorrectionStatus getStatus() {
        return status;
    }

    public long getSubmittedAtMillis() {
        return submittedAtMillis;
    }
}
