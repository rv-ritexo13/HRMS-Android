package com.triotech.hrms.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.local.HrmsDatabase;
import com.triotech.hrms.data.model.LeaveBalance;
import com.triotech.hrms.data.model.LeaveRequest;
import com.triotech.hrms.data.model.LeaveStatus;
import com.triotech.hrms.data.model.LeaveType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link LeaveRepository} backed by the local SQLite {@link HrmsDatabase}. All disk
 * work runs on a single-thread executor with results delivered via
 * {@code LiveData.postValue}, matching the other DB-backed repositories.
 */
public class DbLeaveRepository implements LeaveRepository {

    private static final long SIMULATED_LATENCY_MS = 400L;

    private final HrmsDatabase database;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public DbLeaveRepository(@NonNull Context context) {
        this.database = HrmsDatabase.getInstance(context);
        ioExecutor.execute(database::getReadableDatabase);
    }

    @NonNull
    @Override
    public LiveData<Resource<List<LeaveBalance>>> observeBalances() {
        MutableLiveData<Resource<List<LeaveBalance>>> live = new MutableLiveData<>();
        live.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                List<LeaveBalance> balances = queryBalances();
                live.postValue(balances.isEmpty() ? Resource.empty() : Resource.success(balances));
            } catch (Exception e) {
                live.postValue(Resource.error("Couldn't load your leave balances."));
            }
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<List<LeaveRequest>>> observeRequests() {
        MutableLiveData<Resource<List<LeaveRequest>>> live = new MutableLiveData<>();
        live.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                List<LeaveRequest> requests = queryRequests();
                live.postValue(requests.isEmpty() ? Resource.empty() : Resource.success(requests));
            } catch (Exception e) {
                live.postValue(Resource.error("Couldn't load your leave history."));
            }
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<LeaveRequest>> observeRequest(@NonNull String id) {
        MutableLiveData<Resource<LeaveRequest>> live = new MutableLiveData<>();
        live.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                LeaveRequest request = queryRequest(id);
                live.postValue(request == null ? Resource.error("Leave request not found.")
                        : Resource.success(request));
            } catch (Exception e) {
                live.postValue(Resource.error("Couldn't load this leave request."));
            }
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<LeaveRequest>> applyLeave(@NonNull LeaveRequest draft) {
        MutableLiveData<Resource<LeaveRequest>> live = new MutableLiveData<>();
        live.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                SQLiteDatabase db = database.getWritableDatabase();
                long rowId = db.insert(HrmsDatabase.TABLE_LEAVE_REQUESTS, null, toValues(draft));
                live.postValue(rowId == -1 ? Resource.error("Couldn't submit your leave request.")
                        : Resource.success(draft));
            } catch (Exception e) {
                live.postValue(Resource.error("Couldn't submit your leave request."));
            }
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<Boolean>> cancelRequest(@NonNull String id) {
        MutableLiveData<Resource<Boolean>> live = new MutableLiveData<>();
        live.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                SQLiteDatabase db = database.getWritableDatabase();
                ContentValues v = new ContentValues();
                v.put(HrmsDatabase.COL_LR_STATUS, LeaveStatus.CANCELLED.key());
                int rows = db.update(HrmsDatabase.TABLE_LEAVE_REQUESTS, v,
                        HrmsDatabase.COL_LR_ID + " = ? AND " + HrmsDatabase.COL_LR_STATUS + " = ?",
                        new String[] {id, LeaveStatus.PENDING.key()});
                live.postValue(rows == 0 ? Resource.error("This request can no longer be cancelled.")
                        : Resource.success(Boolean.TRUE));
            } catch (Exception e) {
                live.postValue(Resource.error("Couldn't cancel this request."));
            }
        });
        return live;
    }

    @NonNull
    private List<LeaveBalance> queryBalances() {
        List<LeaveBalance> result = new ArrayList<>();
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor c = db.query(HrmsDatabase.TABLE_LEAVE_BALANCES, null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                LeaveType type = LeaveType.fromKey(c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_LB_TYPE)));
                if (type == null) {
                    continue;
                }
                result.add(new LeaveBalance(type,
                        c.getInt(c.getColumnIndexOrThrow(HrmsDatabase.COL_LB_TOTAL)),
                        c.getInt(c.getColumnIndexOrThrow(HrmsDatabase.COL_LB_USED))));
            }
        }
        // Keep a stable, meaningful order regardless of row order.
        result.sort((a, b) -> Integer.compare(a.getType().ordinal(), b.getType().ordinal()));
        return result;
    }

    @NonNull
    private List<LeaveRequest> queryRequests() {
        List<LeaveRequest> result = new ArrayList<>();
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor c = db.query(HrmsDatabase.TABLE_LEAVE_REQUESTS, null, null, null, null, null,
                HrmsDatabase.COL_LR_APPLIED + " DESC")) {
            while (c.moveToNext()) {
                LeaveRequest request = fromCursor(c);
                if (request != null) {
                    result.add(request);
                }
            }
        }
        return result;
    }

    @Nullable
    private LeaveRequest queryRequest(@NonNull String id) {
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor c = db.query(HrmsDatabase.TABLE_LEAVE_REQUESTS, null,
                HrmsDatabase.COL_LR_ID + " = ?", new String[] {id}, null, null, null)) {
            if (c.moveToFirst()) {
                return fromCursor(c);
            }
        }
        return null;
    }

    @NonNull
    private static ContentValues toValues(@NonNull LeaveRequest r) {
        ContentValues v = new ContentValues();
        v.put(HrmsDatabase.COL_LR_ID, r.getId());
        v.put(HrmsDatabase.COL_LR_TYPE, r.getType().key());
        v.put(HrmsDatabase.COL_LR_START, r.getStartMillis());
        v.put(HrmsDatabase.COL_LR_END, r.getEndMillis());
        v.put(HrmsDatabase.COL_LR_DAYS, r.getDays());
        v.put(HrmsDatabase.COL_LR_REASON, r.getReason());
        v.put(HrmsDatabase.COL_LR_STATUS, r.getStatus().key());
        v.put(HrmsDatabase.COL_LR_APPLIED, r.getAppliedMillis());
        v.put(HrmsDatabase.COL_LR_MANAGER_COMMENTS, r.getManagerComments());
        v.put(HrmsDatabase.COL_LR_ATTACHMENT, r.getAttachmentName());
        return v;
    }

    @Nullable
    private static LeaveRequest fromCursor(@NonNull Cursor c) {
        LeaveType type = LeaveType.fromKey(c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_LR_TYPE)));
        LeaveStatus status = LeaveStatus.fromKey(c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_LR_STATUS)));
        if (type == null || status == null) {
            return null;
        }
        int attachmentIndex = c.getColumnIndexOrThrow(HrmsDatabase.COL_LR_ATTACHMENT);
        String attachment = c.isNull(attachmentIndex) ? null : c.getString(attachmentIndex);
        return new LeaveRequest(
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_LR_ID)),
                type,
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_LR_START)),
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_LR_END)),
                c.getInt(c.getColumnIndexOrThrow(HrmsDatabase.COL_LR_DAYS)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_LR_REASON)),
                status,
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_LR_APPLIED)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_LR_MANAGER_COMMENTS)),
                attachment);
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(SIMULATED_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
