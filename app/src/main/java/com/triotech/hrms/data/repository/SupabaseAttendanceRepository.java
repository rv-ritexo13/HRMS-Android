package com.triotech.hrms.data.repository;

import android.content.Context;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.network.SupabaseClient;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.LocationUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.AttendanceCorrectionRequest;
import com.triotech.hrms.data.model.AttendanceHistoryEntry;
import com.triotech.hrms.data.model.AttendanceRecord;
import com.triotech.hrms.data.model.AttendanceStatus;
import com.triotech.hrms.data.model.CorrectionStatus;
import com.triotech.hrms.data.model.MonthlyAttendanceSummary;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * {@link AttendanceRepository} backed by Supabase. Today's live state, past-day
 * history, monthly summaries and corrections all read/write the {@code attendance}
 * / {@code attendance_corrections} tables under the signed-in token (RLS-scoped).
 *
 * <p>Check-in records the device's last-known GPS fix (lat/lng + a reverse-geocoded
 * place) but never blocks: if location permission is off or no fix is available,
 * the punch still goes through without coordinates. Location work + geocoding run
 * off the main thread.</p>
 */
public class SupabaseAttendanceRepository implements AttendanceRepository {

    private static final int EXPECTED_HOUR = 9;
    private static final int EXPECTED_MINUTE = 30;
    private static final int LATE_GRACE_MINUTES = 15;

    private final Context appContext;
    private final SupabaseClient client = SupabaseClient.getInstance();
    private final ExecutorService bg = Executors.newSingleThreadExecutor();

    private final MutableLiveData<AttendanceRecord> today =
            new MutableLiveData<>(new AttendanceRecord(AttendanceRecord.Status.NOT_CHECKED_IN, null, null));
    private final MutableLiveData<String> pendingMissingCheckout = new MutableLiveData<>(null);
    private final MutableLiveData<Resource<List<AttendanceCorrectionRequest>>> corrections = new MutableLiveData<>();
    private boolean correctionsLoaded;

    public SupabaseAttendanceRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    // ===================== Today =====================

    @NonNull
    @Override
    public LiveData<AttendanceRecord> observeToday() {
        refreshToday();
        queryPendingMissingCheckout();
        return today;
    }

    private void refreshToday() {
        client.get("attendance?select=*&date_key=eq." + todayKey(), resp -> {
            if (!resp.isSuccess()) {
                return;
            }
            List<JSONObject> rows = rows(resp.body);
            today.setValue(rows.isEmpty()
                    ? new AttendanceRecord(AttendanceRecord.Status.NOT_CHECKED_IN, null, null)
                    : toRecord(rows.get(0)));
        });
    }

    @NonNull
    @Override
    public LiveData<Resource<AttendanceRecord>> checkIn() {
        MutableLiveData<Resource<AttendanceRecord>> result = new MutableLiveData<>();
        AttendanceRecord current = today.getValue();
        if (current != null && !current.canCheckIn()) {
            result.setValue(Resource.error(current.getStatus() == AttendanceRecord.Status.CHECKED_OUT
                    ? "You've already completed attendance for today"
                    : "You're already checked in for today"));
            return result;
        }
        result.setValue(Resource.loading());
        bg.execute(() -> {
            long now = System.currentTimeMillis();
            JSONObject body = new JSONObject();
            try {
                body.put("id", UUID.randomUUID().toString());
                body.put("date_key", todayKey());
                body.put("date_millis", DateUtils.today().getTimeInMillis());
                body.put("status", deriveStatus(now).name());
                body.put("check_in_millis", now);
                putLocation(body, "check_in", LocationUtils.lastKnown(appContext));
            } catch (Exception ignored) {
                // keys are constant
            }
            client.post("attendance", body.toString(), resp -> {
                if (resp.isSuccess()) {
                    AttendanceRecord rec = new AttendanceRecord(AttendanceRecord.Status.CHECKED_IN, now, null);
                    today.setValue(rec);
                    result.setValue(Resource.success(rec));
                } else {
                    result.setValue(Resource.error("Couldn't check you in. Please try again."));
                }
            });
        });
        return result;
    }

    @NonNull
    @Override
    public LiveData<Resource<AttendanceRecord>> checkOut() {
        MutableLiveData<Resource<AttendanceRecord>> result = new MutableLiveData<>();
        AttendanceRecord current = today.getValue();
        if (current == null || !current.canCheckOut()) {
            result.setValue(Resource.error(current != null
                    && current.getStatus() == AttendanceRecord.Status.NOT_CHECKED_IN
                    ? "Please check in before you check out"
                    : "You've already checked out today"));
            return result;
        }
        final Long checkIn = current.getCheckInTimeMillis();
        result.setValue(Resource.loading());
        bg.execute(() -> {
            long now = System.currentTimeMillis();
            JSONObject body = new JSONObject();
            try {
                body.put("check_out_millis", now);
                putLocation(body, "check_out", LocationUtils.lastKnown(appContext));
            } catch (Exception ignored) {
                // keys are constant
            }
            client.patch("attendance?date_key=eq." + todayKey(), body.toString(), resp -> {
                if (resp.isSuccess()) {
                    AttendanceRecord rec = new AttendanceRecord(AttendanceRecord.Status.CHECKED_OUT, checkIn, now);
                    today.setValue(rec);
                    result.setValue(Resource.success(rec));
                } else {
                    result.setValue(Resource.error("Couldn't check you out. Please try again."));
                }
            });
        });
        return result;
    }

    @NonNull
    @Override
    public LiveData<String> observePendingMissingCheckout() {
        queryPendingMissingCheckout();
        return pendingMissingCheckout;
    }

    private void queryPendingMissingCheckout() {
        client.get("attendance?select=date_key&check_in_millis=not.is.null&check_out_millis=is.null&date_key=lt."
                + todayKey() + "&order=date_key.desc&limit=1", resp -> {
            if (!resp.isSuccess()) {
                return;
            }
            List<JSONObject> rows = rows(resp.body);
            pendingMissingCheckout.setValue(rows.isEmpty() ? null : rows.get(0).optString("date_key", null));
        });
    }

    // ===================== History / summary =====================

    @NonNull
    @Override
    public LiveData<Resource<List<AttendanceHistoryEntry>>> observeHistory(int year, int month) {
        MutableLiveData<Resource<List<AttendanceHistoryEntry>>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get(monthQuery(year, month), resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load your attendance history."));
                return;
            }
            List<AttendanceHistoryEntry> entries = new ArrayList<>();
            for (JSONObject o : rows(resp.body)) {
                AttendanceStatus status = safeStatus(o.optString("status", null));
                if (status != null) {
                    entries.add(new AttendanceHistoryEntry(
                            o.optString("date_key"), o.optLong("date_millis"), status,
                            nullableLong(o, "check_in_millis"), nullableLong(o, "check_out_millis")));
                }
            }
            live.setValue(entries.isEmpty() ? Resource.empty() : Resource.success(entries));
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<MonthlyAttendanceSummary>> observeMonthlySummary(int year, int month) {
        MutableLiveData<Resource<MonthlyAttendanceSummary>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get(monthQuery(year, month), resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load your monthly summary."));
                return;
            }
            int present = 0, late = 0, half = 0, absent = 0, leave = 0, wfh = 0;
            for (JSONObject o : rows(resp.body)) {
                AttendanceStatus status = safeStatus(o.optString("status", null));
                if (status == null) {
                    continue;
                }
                switch (status) {
                    case PRESENT: present++; break;
                    case LATE: late++; break;
                    case HALF_DAY: half++; break;
                    case ABSENT: absent++; break;
                    case LEAVE: leave++; break;
                    case WORK_FROM_HOME: wfh++; break;
                }
            }
            live.setValue(Resource.success(new MonthlyAttendanceSummary(present, late, half, absent, leave, wfh)));
        });
        return live;
    }

    @NonNull
    private String monthQuery(int year, int month) {
        String prefix = String.format(Locale.US, "%04d-%02d", year, month);
        // like.<prefix>-* matches every day of that month; exclude today (shown live).
        return "attendance?select=*&date_key=like." + prefix + "-*&date_key=neq." + todayKey()
                + "&order=date_key.asc";
    }

    // ===================== Corrections =====================

    @NonNull
    @Override
    public LiveData<Resource<AttendanceCorrectionRequest>> submitCorrection(
            @NonNull String dateKey, long dateMillis, @Nullable Long actualCheckInMillis,
            @Nullable Long expectedCheckInMillis, @NonNull String reason) {
        MutableLiveData<Resource<AttendanceCorrectionRequest>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        long now = System.currentTimeMillis();
        String id = UUID.randomUUID().toString();
        JSONObject body = new JSONObject();
        try {
            body.put("id", id);
            body.put("date_key", dateKey);
            body.put("date_millis", dateMillis);
            body.put("actual_check_in_millis", actualCheckInMillis == null ? JSONObject.NULL : actualCheckInMillis);
            body.put("expected_check_in_millis",
                    expectedCheckInMillis == null ? JSONObject.NULL : expectedCheckInMillis);
            body.put("reason", reason);
            body.put("status", CorrectionStatus.PENDING.name());
            body.put("created_millis", now);
        } catch (Exception ignored) {
            // keys are constant
        }
        client.post("attendance_corrections", body.toString(), resp -> {
            if (!resp.isSuccess()) {
                result.setValue(Resource.error("Couldn't submit your correction. Please try again."));
                return;
            }
            AttendanceCorrectionRequest request = new AttendanceCorrectionRequest(
                    id, dateKey, dateMillis, actualCheckInMillis, expectedCheckInMillis, reason,
                    CorrectionStatus.PENDING, now);
            refreshCorrections();
            if (dateKey.equals(pendingMissingCheckout.getValue())) {
                pendingMissingCheckout.setValue(null);
            }
            result.setValue(Resource.success(request));
        });
        return result;
    }

    @NonNull
    @Override
    public LiveData<Resource<List<AttendanceCorrectionRequest>>> observeCorrections() {
        if (!correctionsLoaded) {
            correctionsLoaded = true;
            refreshCorrections();
        }
        return corrections;
    }

    private void refreshCorrections() {
        client.get("attendance_corrections?select=*&order=created_millis.desc", resp -> {
            if (!resp.isSuccess()) {
                corrections.setValue(Resource.error("Couldn't load your corrections."));
                return;
            }
            List<AttendanceCorrectionRequest> list = new ArrayList<>();
            for (JSONObject o : rows(resp.body)) {
                CorrectionStatus status = safeCorrectionStatus(o.optString("status", null));
                if (status != null) {
                    list.add(new AttendanceCorrectionRequest(
                            o.optString("id"), o.optString("date_key"), o.optLong("date_millis"),
                            nullableLong(o, "actual_check_in_millis"), nullableLong(o, "expected_check_in_millis"),
                            o.optString("reason", ""), status, o.optLong("created_millis")));
                }
            }
            corrections.setValue(list.isEmpty() ? Resource.success(new ArrayList<>()) : Resource.success(list));
        });
    }

    // ===================== helpers =====================

    @NonNull
    private static AttendanceRecord toRecord(@NonNull JSONObject o) {
        Long checkIn = nullableLong(o, "check_in_millis");
        Long checkOut = nullableLong(o, "check_out_millis");
        AttendanceRecord.Status status = checkOut != null ? AttendanceRecord.Status.CHECKED_OUT
                : checkIn != null ? AttendanceRecord.Status.CHECKED_IN
                : AttendanceRecord.Status.NOT_CHECKED_IN;
        return new AttendanceRecord(status, checkIn, checkOut);
    }

    private static void putLocation(@NonNull JSONObject body, @NonNull String prefix, @Nullable Location loc) {
        if (loc == null) {
            return;
        }
        try {
            body.put(prefix + "_lat", loc.getLatitude());
            body.put(prefix + "_lng", loc.getLongitude());
        } catch (Exception ignored) {
            // constant keys
        }
    }

    @NonNull
    private static String todayKey() {
        return DateUtils.dateKey(DateUtils.today());
    }

    @NonNull
    private static AttendanceStatus deriveStatus(long checkInMillis) {
        Calendar c = DateUtils.calendarFromMillis(checkInMillis);
        int minutesAfterExpected = (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE))
                - (EXPECTED_HOUR * 60 + EXPECTED_MINUTE);
        return minutesAfterExpected > LATE_GRACE_MINUTES ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
    }

    @Nullable
    private static Long nullableLong(@NonNull JSONObject o, @NonNull String key) {
        return o.isNull(key) ? null : o.optLong(key);
    }

    @Nullable
    private static AttendanceStatus safeStatus(@Nullable String s) {
        try {
            return s == null ? null : AttendanceStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    private static CorrectionStatus safeCorrectionStatus(@Nullable String s) {
        try {
            return s == null ? null : CorrectionStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @NonNull
    private static List<JSONObject> rows(@NonNull String json) {
        List<JSONObject> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                out.add(arr.getJSONObject(i));
            }
        } catch (Exception ignored) {
            // Return whatever parsed.
        }
        return out;
    }
}
