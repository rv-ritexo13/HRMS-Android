package com.triotech.hrms.ui.attendance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.AttendanceCorrectionRequest;
import com.triotech.hrms.data.model.AttendanceHistoryEntry;
import com.triotech.hrms.data.model.AttendanceRecord;
import com.triotech.hrms.data.model.MonthlyAttendanceSummary;
import com.triotech.hrms.data.repository.AttendanceRepository;
import java.util.Calendar;
import java.util.List;

/**
 * Backs the Attendance tab: today's live check-in/check-out card, a selectable
 * month for the History list / Calendar / Monthly Summary (all three stay in
 * sync since they share {@link #selectedMonth}), and correction requests.
 */
public class AttendanceViewModel extends BaseViewModel {

    private final AttendanceRepository repository;

    private final LiveData<AttendanceRecord> today;
    private final LiveData<String> pendingMissingCheckout;
    private final LiveData<Resource<List<AttendanceCorrectionRequest>>> corrections;

    private final MutableLiveData<int[]> selectedMonth;
    private final MediatorLiveData<Resource<List<AttendanceHistoryEntry>>> history = new MediatorLiveData<>();
    private final MediatorLiveData<Resource<MonthlyAttendanceSummary>> summary = new MediatorLiveData<>();
    @Nullable private LiveData<Resource<List<AttendanceHistoryEntry>>> historySource;
    @Nullable private LiveData<Resource<MonthlyAttendanceSummary>> summarySource;

    public AttendanceViewModel(@NonNull AttendanceRepository repository) {
        this.repository = repository;
        this.today = repository.observeToday();
        this.pendingMissingCheckout = repository.observePendingMissingCheckout();
        this.corrections = repository.observeCorrections();

        Calendar now = DateUtils.today();
        this.selectedMonth = new MutableLiveData<>(new int[] {now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1});
        loadMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1);
    }

    @NonNull
    public LiveData<AttendanceRecord> getToday() {
        return today;
    }

    @NonNull
    public LiveData<String> getPendingMissingCheckout() {
        return pendingMissingCheckout;
    }

    @NonNull
    public LiveData<Resource<List<AttendanceCorrectionRequest>>> getCorrections() {
        return corrections;
    }

    @NonNull
    public LiveData<int[]> getSelectedMonth() {
        return selectedMonth;
    }

    @NonNull
    public LiveData<Resource<List<AttendanceHistoryEntry>>> getHistory() {
        return history;
    }

    @NonNull
    public LiveData<Resource<MonthlyAttendanceSummary>> getSummary() {
        return summary;
    }

    public void selectMonth(int year, int month) {
        selectedMonth.setValue(new int[] {year, month});
        loadMonth(year, month);
    }

    public void goToPreviousMonth() {
        int[] current = currentMonthOrToday();
        int[] previous = DateUtils.addMonths(current[0], current[1], -1);
        selectMonth(previous[0], previous[1]);
    }

    /** No-ops once the selected month is the current month — history has nothing beyond today. */
    public void goToNextMonth() {
        int[] current = currentMonthOrToday();
        Calendar now = DateUtils.today();
        if (current[0] == now.get(Calendar.YEAR) && current[1] == now.get(Calendar.MONTH) + 1) {
            return;
        }
        int[] next = DateUtils.addMonths(current[0], current[1], 1);
        selectMonth(next[0], next[1]);
    }

    @NonNull
    private int[] currentMonthOrToday() {
        int[] value = selectedMonth.getValue();
        if (value != null) {
            return value;
        }
        Calendar now = DateUtils.today();
        return new int[] {now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1};
    }

    private void loadMonth(int year, int month) {
        if (historySource != null) {
            history.removeSource(historySource);
        }
        historySource = repository.observeHistory(year, month);
        history.addSource(historySource, history::setValue);

        if (summarySource != null) {
            summary.removeSource(summarySource);
        }
        summarySource = repository.observeMonthlySummary(year, month);
        summary.addSource(summarySource, summary::setValue);
    }

    /** Fresh, one-shot LiveData for the check-in action's loading/success/error state. */
    @NonNull
    public LiveData<Resource<AttendanceRecord>> checkIn() {
        return repository.checkIn();
    }

    /** Fresh, one-shot LiveData for the check-out action's loading/success/error state. */
    @NonNull
    public LiveData<Resource<AttendanceRecord>> checkOut() {
        return repository.checkOut();
    }

    @NonNull
    public LiveData<Resource<AttendanceCorrectionRequest>> submitCorrection(
            @NonNull String dateKey,
            long dateMillis,
            @Nullable Long actualCheckInMillis,
            @Nullable Long expectedCheckInMillis,
            @NonNull String reason) {
        return repository.submitCorrection(dateKey, dateMillis, actualCheckInMillis, expectedCheckInMillis, reason);
    }
}
