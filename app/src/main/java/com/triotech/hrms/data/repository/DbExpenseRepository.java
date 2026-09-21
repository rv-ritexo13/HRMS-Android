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
import com.triotech.hrms.data.model.Expense;
import com.triotech.hrms.data.model.ExpenseCategory;
import com.triotech.hrms.data.model.ExpenseStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link ExpenseRepository} backed by the local SQLite {@link HrmsDatabase}. All
 * disk work runs on a single-thread executor with results delivered via
 * {@code LiveData.postValue}, matching {@link DbLeaveRepository} and the other
 * DB-backed repositories.
 */
public class DbExpenseRepository implements ExpenseRepository {

    private static final long SIMULATED_LATENCY_MS = 400L;

    private final HrmsDatabase database;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public DbExpenseRepository(@NonNull Context context) {
        this.database = HrmsDatabase.getInstance(context);
        ioExecutor.execute(database::getReadableDatabase);
    }

    @NonNull
    @Override
    public LiveData<Resource<List<Expense>>> observeExpenses() {
        MutableLiveData<Resource<List<Expense>>> live = new MutableLiveData<>();
        live.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                List<Expense> expenses = queryExpenses();
                live.postValue(expenses.isEmpty() ? Resource.empty() : Resource.success(expenses));
            } catch (Exception e) {
                live.postValue(Resource.error("Couldn't load your expenses."));
            }
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<Expense>> observeExpense(@NonNull String id) {
        MutableLiveData<Resource<Expense>> live = new MutableLiveData<>();
        live.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                Expense expense = queryExpense(id);
                live.postValue(expense == null ? Resource.error("Expense not found.")
                        : Resource.success(expense));
            } catch (Exception e) {
                live.postValue(Resource.error("Couldn't load this expense."));
            }
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<Expense>> saveExpense(@NonNull Expense expense) {
        MutableLiveData<Resource<Expense>> live = new MutableLiveData<>();
        live.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                SQLiteDatabase db = database.getWritableDatabase();
                long rowId = db.insert(HrmsDatabase.TABLE_EXPENSES, null, toValues(expense));
                live.postValue(rowId == -1 ? Resource.error("Couldn't save your expense.")
                        : Resource.success(expense));
            } catch (Exception e) {
                live.postValue(Resource.error("Couldn't save your expense."));
            }
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<Expense>> updateExpense(@NonNull Expense expense) {
        MutableLiveData<Resource<Expense>> live = new MutableLiveData<>();
        live.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                SQLiteDatabase db = database.getWritableDatabase();
                int rows = db.update(HrmsDatabase.TABLE_EXPENSES, toValues(expense),
                        HrmsDatabase.COL_EXP_ID + " = ?", new String[] {expense.getId()});
                live.postValue(rows == 0 ? Resource.error("Couldn't update your expense.")
                        : Resource.success(expense));
            } catch (Exception e) {
                live.postValue(Resource.error("Couldn't update your expense."));
            }
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<Boolean>> submitExpense(@NonNull String id) {
        MutableLiveData<Resource<Boolean>> live = new MutableLiveData<>();
        live.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                SQLiteDatabase db = database.getWritableDatabase();
                ContentValues v = new ContentValues();
                v.put(HrmsDatabase.COL_EXP_STATUS, ExpenseStatus.SUBMITTED.key());
                int rows = db.update(HrmsDatabase.TABLE_EXPENSES, v,
                        HrmsDatabase.COL_EXP_ID + " = ? AND " + HrmsDatabase.COL_EXP_STATUS + " = ?",
                        new String[] {id, ExpenseStatus.DRAFT.key()});
                live.postValue(rows == 0 ? Resource.error("This expense can no longer be submitted.")
                        : Resource.success(Boolean.TRUE));
            } catch (Exception e) {
                live.postValue(Resource.error("Couldn't submit this expense."));
            }
        });
        return live;
    }

    @NonNull
    private List<Expense> queryExpenses() {
        List<Expense> result = new ArrayList<>();
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor c = db.query(HrmsDatabase.TABLE_EXPENSES, null, null, null, null, null,
                HrmsDatabase.COL_EXP_DATE + " DESC")) {
            while (c.moveToNext()) {
                Expense expense = fromCursor(c);
                if (expense != null) {
                    result.add(expense);
                }
            }
        }
        return result;
    }

    @Nullable
    private Expense queryExpense(@NonNull String id) {
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor c = db.query(HrmsDatabase.TABLE_EXPENSES, null,
                HrmsDatabase.COL_EXP_ID + " = ?", new String[] {id}, null, null, null)) {
            if (c.moveToFirst()) {
                return fromCursor(c);
            }
        }
        return null;
    }

    @NonNull
    private static ContentValues toValues(@NonNull Expense e) {
        ContentValues v = new ContentValues();
        v.put(HrmsDatabase.COL_EXP_ID, e.getId());
        v.put(HrmsDatabase.COL_EXP_TITLE, e.getTitle());
        v.put(HrmsDatabase.COL_EXP_CATEGORY, e.getCategory().key());
        v.put(HrmsDatabase.COL_EXP_AMOUNT, e.getAmount());
        v.put(HrmsDatabase.COL_EXP_DATE, e.getDateMillis());
        v.put(HrmsDatabase.COL_EXP_DESCRIPTION, e.getDescription());
        v.put(HrmsDatabase.COL_EXP_STATUS, e.getStatus().key());
        v.put(HrmsDatabase.COL_EXP_CREATED, e.getCreatedMillis());
        v.put(HrmsDatabase.COL_EXP_RECEIPT, e.getReceiptName());
        return v;
    }

    @Nullable
    private static Expense fromCursor(@NonNull Cursor c) {
        ExpenseCategory category =
                ExpenseCategory.fromKey(c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_EXP_CATEGORY)));
        ExpenseStatus status =
                ExpenseStatus.fromKey(c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_EXP_STATUS)));
        if (category == null || status == null) {
            return null;
        }
        int receiptIndex = c.getColumnIndexOrThrow(HrmsDatabase.COL_EXP_RECEIPT);
        String receipt = c.isNull(receiptIndex) ? null : c.getString(receiptIndex);
        return new Expense(
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_EXP_ID)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_EXP_TITLE)),
                category,
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_EXP_AMOUNT)),
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_EXP_DATE)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_EXP_DESCRIPTION)),
                status,
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_EXP_CREATED)),
                receipt);
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(SIMULATED_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
