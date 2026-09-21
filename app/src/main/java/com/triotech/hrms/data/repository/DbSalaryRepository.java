package com.triotech.hrms.data.repository;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.local.HrmsDatabase;
import com.triotech.hrms.data.model.Payslip;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link SalaryRepository} backed by the local SQLite {@link HrmsDatabase}.
 *
 * <p>All disk access runs on a single-thread {@link ExecutorService} so the UI
 * thread never touches SQLite; results are delivered back via
 * {@code LiveData.postValue}. A short artificial pause keeps the Loading state
 * visible, consistent with the other Fake repositories in the app.</p>
 */
public class DbSalaryRepository implements SalaryRepository {

    private static final long SIMULATED_LATENCY_MS = 400L;

    private final HrmsDatabase database;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public DbSalaryRepository(@NonNull Context context) {
        this.database = HrmsDatabase.getInstance(context);
        // Touch the DB off the main thread so it's created/seeded eagerly and is
        // immediately visible in the Database Inspector after launch.
        ioExecutor.execute(database::getReadableDatabase);
    }

    @NonNull
    @Override
    public LiveData<Resource<List<Payslip>>> observeSalaryHistory() {
        MutableLiveData<Resource<List<Payslip>>> liveData = new MutableLiveData<>();
        liveData.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                List<Payslip> payslips = queryAll();
                liveData.postValue(payslips.isEmpty() ? Resource.empty() : Resource.success(payslips));
            } catch (Exception e) {
                liveData.postValue(Resource.error("Couldn't load your salary history."));
            }
        });
        return liveData;
    }

    @NonNull
    @Override
    public LiveData<Resource<Payslip>> observePayslip(@NonNull String monthKey) {
        MutableLiveData<Resource<Payslip>> liveData = new MutableLiveData<>();
        liveData.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                Payslip payslip = queryByKey(monthKey);
                if (payslip == null) {
                    liveData.postValue(Resource.error("Payslip not found."));
                } else {
                    liveData.postValue(Resource.success(payslip));
                }
            } catch (Exception e) {
                liveData.postValue(Resource.error("Couldn't load this payslip."));
            }
        });
        return liveData;
    }

    @NonNull
    private List<Payslip> queryAll() {
        List<Payslip> result = new ArrayList<>();
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.query(
                HrmsDatabase.TABLE_PAYSLIPS, null, null, null, null, null,
                HrmsDatabase.COL_MONTH_KEY + " DESC")) {
            while (cursor.moveToNext()) {
                result.add(fromCursor(cursor));
            }
        }
        return result;
    }

    private Payslip queryByKey(@NonNull String monthKey) {
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor cursor = db.query(
                HrmsDatabase.TABLE_PAYSLIPS, null,
                HrmsDatabase.COL_MONTH_KEY + " = ?", new String[] {monthKey},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
        }
        return null;
    }

    @NonNull
    private static Payslip fromCursor(@NonNull Cursor c) {
        return new Payslip(
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_MONTH_KEY)),
                c.getInt(c.getColumnIndexOrThrow(HrmsDatabase.COL_YEAR)),
                c.getInt(c.getColumnIndexOrThrow(HrmsDatabase.COL_MONTH)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_EMPLOYEE_ID)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_EMPLOYEE_NAME)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_DEPARTMENT)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_DESIGNATION)),
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_BASIC)),
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_HRA)),
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_SPECIAL_ALLOWANCE)),
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_OTHER_ALLOWANCES)),
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROVIDENT_FUND)),
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFESSIONAL_TAX)),
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_OTHER_DEDUCTIONS)),
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_CREDIT_DATE)),
                c.getInt(c.getColumnIndexOrThrow(HrmsDatabase.COL_CREDITED)) == 1);
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(SIMULATED_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
