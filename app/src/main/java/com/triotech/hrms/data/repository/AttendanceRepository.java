package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.AttendanceCorrectionRequest;
import com.triotech.hrms.data.model.AttendanceHistoryEntry;
import com.triotech.hrms.data.model.AttendanceRecord;
import com.triotech.hrms.data.model.MonthlyAttendanceSummary;
import java.util.List;

/**
 * The full attendance module's data contract: today's live check-in/check-out
 * state, past-day history, monthly summaries, and correction requests.
 * {@link FakeAttendanceRepository} backs this with mock/local data for Phase 3;
 * a real implementation (server-backed clock, a timesheet API, and — in a later
 * phase — GPS) can replace it behind {@link com.triotech.hrms.core.di.ServiceLocator}
 * without any UI-layer changes, since every method already returns
 * {@code LiveData<Resource<T>>} or a plain {@code LiveData<T>}.
 */
public interface AttendanceRepository {

    /** Live view of today's current attendance state, updated by check-in/check-out. */
    @NonNull
    LiveData<AttendanceRecord> observeToday();

    /** Records a check-in "now". Emits loading, then success or a specific error (see edge cases). */
    @NonNull
    LiveData<Resource<AttendanceRecord>> checkIn();

    /** Records a check-out "now". Emits loading, then success or a specific error (see edge cases). */
    @NonNull
    LiveData<Resource<AttendanceRecord>> checkOut();

    /** Past-day attendance for the given year / 1-12 month. Never includes today or future days. */
    @NonNull
    LiveData<Resource<List<AttendanceHistoryEntry>>> observeHistory(int year, int month);

    /** Aggregate day-counts for the given year / 1-12 month, derived from the same data as {@link #observeHistory}. */
    @NonNull
    LiveData<Resource<MonthlyAttendanceSummary>> observeMonthlySummary(int year, int month);

    /**
     * The "yyyy-MM-dd" key of a prior day left checked-in with no check-out (e.g. the app was
     * killed before the employee checked out), or {@code null} once none is pending. Surfaced so
     * the Attendance screen can prompt for a correction instead of silently losing the day.
     */
    @NonNull
    LiveData<String> observePendingMissingCheckout();

    /** Submits a correction request for a specific day. Emits loading, then success or error. */
    @NonNull
    LiveData<Resource<AttendanceCorrectionRequest>> submitCorrection(
            @NonNull String dateKey,
            long dateMillis,
            @Nullable Long actualCheckInMillis,
            @Nullable Long expectedCheckInMillis,
            @NonNull String reason);

    /** All correction requests the employee has submitted, most recent first. */
    @NonNull
    LiveData<Resource<List<AttendanceCorrectionRequest>>> observeCorrections();
}
