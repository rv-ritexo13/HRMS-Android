package com.triotech.hrms.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.AttendanceCorrectionRequest;
import com.triotech.hrms.data.model.AttendanceHistoryEntry;
import com.triotech.hrms.data.model.AttendanceRecord;
import com.triotech.hrms.data.model.AttendanceStatus;
import com.triotech.hrms.data.model.CorrectionStatus;
import com.triotech.hrms.data.model.MonthlyAttendanceSummary;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * In-memory + SharedPreferences-backed mock for the whole attendance module.
 *
 * <p><b>Today</b> is the single source of truth the Home dashboard and the
 * Attendance screen both observe. It is persisted to SharedPreferences keyed by
 * date so a check-in survives an app process restart (killed and reopened the
 * same day resumes "Checked In"); a fresh calendar day always resets it to
 * {@code NOT_CHECKED_IN}, and if the previous day was left checked-in with no
 * check-out, that gap is surfaced via {@link #observePendingMissingCheckout()}
 * rather than silently discarded.</p>
 *
 * <p><b>History</b> is deterministic mock data generated per month on first
 * request (seeded by year/month, so repeated visits to the same month are
 * stable) and deliberately excludes today — today's state is already shown live
 * by {@link #observeToday()}, so including it here would just duplicate it.</p>
 *
 * <p>A real backend can replace this class behind {@link com.triotech.hrms.core.di.ServiceLocator}
 * without touching any ViewModel or Fragment — every method already returns the
 * same {@code LiveData<Resource<T>>} shapes a network-backed implementation would.</p>
 */
public class FakeAttendanceRepository implements AttendanceRepository {

    private static final long SIMULATED_LATENCY_MS = 500L;
    private static final long HISTORY_LATENCY_MS = 300L;

    private static final String PREFS_NAME = "hrms_attendance_prefs";
    private static final String KEY_DATE = "today_date_key";
    private static final String KEY_STATUS = "today_status";
    private static final String KEY_CHECK_IN = "today_check_in_millis";
    private static final String KEY_CHECK_OUT = "today_check_out_millis";
    private static final long NO_VALUE = -1L;

    // Expected start of day is 09:30; check-ins after 09:45 count as Late.
    private static final int EXPECTED_HOUR = 9;
    private static final int EXPECTED_MINUTE = 30;
    private static final int LATE_GRACE_MINUTES = 15;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @Nullable private final Context appContext;

    private final MutableLiveData<AttendanceRecord> today;
    private final MutableLiveData<String> pendingMissingCheckout = new MutableLiveData<>(null);

    private final Map<String, List<AttendanceHistoryEntry>> historyByMonth = new HashMap<>();
    @Nullable private AttendanceHistoryEntry pendingMissingCheckoutEntry;

    private final List<AttendanceCorrectionRequest> corrections = new ArrayList<>();
    private final MutableLiveData<Resource<List<AttendanceCorrectionRequest>>> correctionsLiveData =
            new MutableLiveData<>();

    public FakeAttendanceRepository(@Nullable Context context) {
        this.appContext = context;
        this.today = new MutableLiveData<>(restoreOrResetToday());
        seedSampleCorrections();
    }

    // ===================== Today: check-in / check-out =====================

    @NonNull
    @Override
    public LiveData<AttendanceRecord> observeToday() {
        return today;
    }

    @NonNull
    @Override
    public LiveData<Resource<AttendanceRecord>> checkIn() {
        MutableLiveData<Resource<AttendanceRecord>> result = new MutableLiveData<>();
        AttendanceRecord current = today.getValue();
        if (current == null || !current.canCheckIn()) {
            String message = current != null && current.getStatus() == AttendanceRecord.Status.CHECKED_OUT
                    ? "You've already completed attendance for today"
                    : "You're already checked in for today";
            result.setValue(Resource.error(message));
            return result;
        }
        result.setValue(Resource.loading());
        mainHandler.postDelayed(() -> {
            AttendanceRecord updated =
                    new AttendanceRecord(AttendanceRecord.Status.CHECKED_IN, System.currentTimeMillis(), null);
            today.setValue(updated);
            persistToday(updated);
            result.setValue(Resource.success(updated));
        }, SIMULATED_LATENCY_MS);
        return result;
    }

    @NonNull
    @Override
    public LiveData<Resource<AttendanceRecord>> checkOut() {
        MutableLiveData<Resource<AttendanceRecord>> result = new MutableLiveData<>();
        AttendanceRecord current = today.getValue();
        if (current == null || !current.canCheckOut()) {
            String message = current != null && current.getStatus() == AttendanceRecord.Status.NOT_CHECKED_IN
                    ? "Please check in before you check out"
                    : "You've already checked out today";
            result.setValue(Resource.error(message));
            return result;
        }
        result.setValue(Resource.loading());
        mainHandler.postDelayed(() -> {
            AttendanceRecord updated = new AttendanceRecord(
                    AttendanceRecord.Status.CHECKED_OUT, current.getCheckInTimeMillis(), System.currentTimeMillis());
            today.setValue(updated);
            persistToday(updated);
            result.setValue(Resource.success(updated));
        }, SIMULATED_LATENCY_MS);
        return result;
    }

    @NonNull
    @Override
    public LiveData<String> observePendingMissingCheckout() {
        return pendingMissingCheckout;
    }

    /**
     * Loads whatever was persisted for "today" from a prior process. If the persisted date
     * differs from today (new day, or nothing was ever saved), the state resets — and if that
     * prior day was left checked-in with no check-out, records it as a pending correction prompt
     * (the "missing check-out" edge case) instead of quietly dropping it.
     */
    @NonNull
    private AttendanceRecord restoreOrResetToday() {
        String todayKey = DateUtils.dateKey(DateUtils.today());
        SharedPreferences prefs = prefsOrNull();
        if (prefs == null) {
            return new AttendanceRecord(AttendanceRecord.Status.NOT_CHECKED_IN, null, null);
        }

        String storedKey = prefs.getString(KEY_DATE, null);
        if (todayKey.equals(storedKey)) {
            AttendanceRecord.Status status = AttendanceRecord.Status.valueOf(
                    prefs.getString(KEY_STATUS, AttendanceRecord.Status.NOT_CHECKED_IN.name()));
            Long checkIn = readNullableLong(prefs, KEY_CHECK_IN);
            Long checkOut = readNullableLong(prefs, KEY_CHECK_OUT);
            return new AttendanceRecord(status, checkIn, checkOut);
        }

        // Different (or no) stored day: a stale "checked in, never checked out" record means the
        // app was closed mid-day. Surface it as a pending correction instead of discarding it.
        if (storedKey != null) {
            AttendanceRecord.Status storedStatus = AttendanceRecord.Status.valueOf(
                    prefs.getString(KEY_STATUS, AttendanceRecord.Status.NOT_CHECKED_IN.name()));
            Long storedCheckIn = readNullableLong(prefs, KEY_CHECK_IN);
            if (storedStatus == AttendanceRecord.Status.CHECKED_IN && storedCheckIn != null) {
                pendingMissingCheckout.setValue(storedKey);
                Calendar staleDay = DateUtils.calendarFromMillis(storedCheckIn);
                pendingMissingCheckoutEntry = new AttendanceHistoryEntry(
                        storedKey, DateUtils.startOfDay(staleDay).getTimeInMillis(),
                        deriveStatus(storedCheckIn), storedCheckIn, null);
            }
        }

        AttendanceRecord fresh = new AttendanceRecord(AttendanceRecord.Status.NOT_CHECKED_IN, null, null);
        persistToday(fresh);
        return fresh;
    }

    private void persistToday(@NonNull AttendanceRecord record) {
        SharedPreferences prefs = prefsOrNull();
        if (prefs == null) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_DATE, DateUtils.dateKey(DateUtils.today()))
                .putString(KEY_STATUS, record.getStatus().name());
        putNullableLong(editor, KEY_CHECK_IN, record.getCheckInTimeMillis());
        putNullableLong(editor, KEY_CHECK_OUT, record.getCheckOutTimeMillis());
        editor.apply();
    }

    @Nullable
    private SharedPreferences prefsOrNull() {
        return appContext == null ? null : appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    private static Long readNullableLong(@NonNull SharedPreferences prefs, @NonNull String key) {
        long value = prefs.getLong(key, NO_VALUE);
        return value == NO_VALUE ? null : value;
    }

    private static void putNullableLong(
            @NonNull SharedPreferences.Editor editor, @NonNull String key, @Nullable Long value) {
        editor.putLong(key, value == null ? NO_VALUE : value);
    }

    // ===================== History / Monthly summary =====================

    @NonNull
    @Override
    public LiveData<Resource<List<AttendanceHistoryEntry>>> observeHistory(int year, int month) {
        MutableLiveData<Resource<List<AttendanceHistoryEntry>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        mainHandler.postDelayed(() -> {
            List<AttendanceHistoryEntry> entries = monthEntries(year, month);
            result.setValue(entries.isEmpty() ? Resource.empty() : Resource.success(entries));
        }, HISTORY_LATENCY_MS);
        return result;
    }

    @NonNull
    @Override
    public LiveData<Resource<MonthlyAttendanceSummary>> observeMonthlySummary(int year, int month) {
        MutableLiveData<Resource<MonthlyAttendanceSummary>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        mainHandler.postDelayed(() -> {
            List<AttendanceHistoryEntry> entries = monthEntries(year, month);
            int present = 0;
            int late = 0;
            int half = 0;
            int absent = 0;
            int leave = 0;
            int wfh = 0;
            for (AttendanceHistoryEntry entry : entries) {
                switch (entry.getStatus()) {
                    case PRESENT:
                        present++;
                        break;
                    case LATE:
                        late++;
                        break;
                    case HALF_DAY:
                        half++;
                        break;
                    case ABSENT:
                        absent++;
                        break;
                    case LEAVE:
                        leave++;
                        break;
                    case WORK_FROM_HOME:
                        wfh++;
                        break;
                }
            }
            result.setValue(Resource.success(new MonthlyAttendanceSummary(present, late, half, absent, leave, wfh)));
        }, HISTORY_LATENCY_MS);
        return result;
    }

    /** Generates (and caches) one month's deterministic mock history, excluding today/future days. */
    @NonNull
    private List<AttendanceHistoryEntry> monthEntries(int year, int month) {
        String monthKey = year + "-" + month;
        List<AttendanceHistoryEntry> cached = historyByMonth.get(monthKey);
        if (cached != null) {
            return cached;
        }

        List<AttendanceHistoryEntry> entries = new ArrayList<>();
        Random random = new Random(year * 100L + month);
        for (Calendar day : DateUtils.daysInMonth(year, month)) {
            if (!DateUtils.isBeforeToday(day) || DateUtils.isWeekend(day)) {
                // "No attendance for the day" for today/future is handled by the live Today card;
                // weekends aren't working days and are excluded from history & the monthly summary.
                continue;
            }
            entries.add(generateDay(day, random));
        }

        if (pendingMissingCheckoutEntry != null) {
            Calendar pendingDay = DateUtils.calendarFromMillis(pendingMissingCheckoutEntry.getDateMillis());
            if (pendingDay.get(Calendar.YEAR) == year && pendingDay.get(Calendar.MONTH) + 1 == month) {
                replaceOrAdd(entries, pendingMissingCheckoutEntry);
            }
        }

        Collections.sort(entries, (a, b) -> Long.compare(a.getDateMillis(), b.getDateMillis()));
        historyByMonth.put(monthKey, entries);
        return entries;
    }

    private static void replaceOrAdd(@NonNull List<AttendanceHistoryEntry> entries, @NonNull AttendanceHistoryEntry entry) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getDateKey().equals(entry.getDateKey())) {
                entries.set(i, entry);
                return;
            }
        }
        entries.add(entry);
    }

    @NonNull
    private AttendanceHistoryEntry generateDay(@NonNull Calendar day, @NonNull Random random) {
        String key = DateUtils.dateKey(day);
        long dateMillis = day.getTimeInMillis();
        int roll = random.nextInt(100);

        if (roll < 4) {
            return new AttendanceHistoryEntry(key, dateMillis, AttendanceStatus.LEAVE, null, null);
        }
        if (roll < 8) {
            long checkIn = timeOn(day, 9, 20 + random.nextInt(20));
            long checkOut = timeOn(day, 18, random.nextInt(30));
            return new AttendanceHistoryEntry(key, dateMillis, AttendanceStatus.WORK_FROM_HOME, checkIn, checkOut);
        }
        if (roll < 12) {
            return new AttendanceHistoryEntry(key, dateMillis, AttendanceStatus.ABSENT, null, null);
        }
        if (roll < 20) {
            long checkIn = timeOn(day, 9, 46 + random.nextInt(60));
            long checkOut = timeOn(day, 18, random.nextInt(40));
            return new AttendanceHistoryEntry(key, dateMillis, AttendanceStatus.LATE, checkIn, checkOut);
        }
        if (roll < 26) {
            long checkIn = timeOn(day, 9, random.nextInt(30));
            long checkOut = timeOn(day, 13, random.nextInt(30));
            return new AttendanceHistoryEntry(key, dateMillis, AttendanceStatus.HALF_DAY, checkIn, checkOut);
        }
        long checkIn = timeOn(day, 9, random.nextInt(30));
        long checkOut = timeOn(day, 18, random.nextInt(45));
        return new AttendanceHistoryEntry(key, dateMillis, AttendanceStatus.PRESENT, checkIn, checkOut);
    }

    private static long timeOn(@NonNull Calendar day, int hour, int minute) {
        Calendar cal = (Calendar) day.clone();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** Present vs. Late, purely from check-in time-of-day. */
    @NonNull
    private static AttendanceStatus deriveStatus(long checkInMillis) {
        Calendar checkIn = DateUtils.calendarFromMillis(checkInMillis);
        int minutesAfterExpected = (checkIn.get(Calendar.HOUR_OF_DAY) * 60 + checkIn.get(Calendar.MINUTE))
                - (EXPECTED_HOUR * 60 + EXPECTED_MINUTE);
        return minutesAfterExpected > LATE_GRACE_MINUTES ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
    }

    // ===================== Corrections =====================

    @NonNull
    @Override
    public LiveData<Resource<AttendanceCorrectionRequest>> submitCorrection(
            @NonNull String dateKey,
            long dateMillis,
            @Nullable Long actualCheckInMillis,
            @Nullable Long expectedCheckInMillis,
            @NonNull String reason) {
        MutableLiveData<Resource<AttendanceCorrectionRequest>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        mainHandler.postDelayed(() -> {
            AttendanceCorrectionRequest request = new AttendanceCorrectionRequest(
                    UUID.randomUUID().toString(), dateKey, dateMillis, actualCheckInMillis, expectedCheckInMillis,
                    reason, CorrectionStatus.PENDING, System.currentTimeMillis());
            corrections.add(0, request);
            correctionsLiveData.setValue(Resource.success(new ArrayList<>(corrections)));

            if (dateKey.equals(pendingMissingCheckout.getValue())) {
                pendingMissingCheckout.setValue(null);
            }
            result.setValue(Resource.success(request));
        }, SIMULATED_LATENCY_MS);
        return result;
    }

    @NonNull
    @Override
    public LiveData<Resource<List<AttendanceCorrectionRequest>>> observeCorrections() {
        return correctionsLiveData;
    }

    /** A couple of pre-seeded requests so the Corrections list isn't empty on first launch. */
    private void seedSampleCorrections() {
        Calendar approvedDay = DateUtils.today();
        approvedDay.add(Calendar.DAY_OF_MONTH, -6);
        Calendar rejectedDay = DateUtils.today();
        rejectedDay.add(Calendar.DAY_OF_MONTH, -3);

        corrections.add(new AttendanceCorrectionRequest(
                UUID.randomUUID().toString(),
                DateUtils.dateKey(approvedDay),
                approvedDay.getTimeInMillis(),
                timeOn(approvedDay, 10, 5),
                timeOn(approvedDay, 9, 30),
                "Traffic delay on the way in",
                CorrectionStatus.APPROVED,
                approvedDay.getTimeInMillis()));

        corrections.add(new AttendanceCorrectionRequest(
                UUID.randomUUID().toString(),
                DateUtils.dateKey(rejectedDay),
                rejectedDay.getTimeInMillis(),
                null,
                timeOn(rejectedDay, 9, 30),
                "Forgot to check in, worked from 9:15 onward",
                CorrectionStatus.REJECTED,
                rejectedDay.getTimeInMillis()));

        correctionsLiveData.setValue(Resource.success(new ArrayList<>(corrections)));
    }
}
