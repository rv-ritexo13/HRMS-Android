package com.triotech.hrms.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.local.HrmsDatabase;
import com.triotech.hrms.data.model.AuthUser;
import com.triotech.hrms.data.model.UserRole;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link AuthRepository} backed by the local {@code users} table. Validates logins
 * against seeded demo accounts and any signed-up accounts, and registers new
 * Employee accounts (also creating a matching profile row so the Profile screen is
 * populated). Passwords are stored in plain text — acceptable only because this is
 * a local mock with no real backend; a production build must hash them server-side.
 */
public class DbAuthRepository implements AuthRepository {

    private static final long SIMULATED_LATENCY_MS = 600L;
    private static final String INVALID_CREDENTIALS = "Invalid Employee ID or password";

    private final HrmsDatabase database;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public DbAuthRepository(@NonNull Context context) {
        this.database = HrmsDatabase.getInstance(context);
        ioExecutor.execute(database::getReadableDatabase);
    }

    @NonNull
    @Override
    public LiveData<Resource<AuthUser>> login(@NonNull String employeeIdOrEmail, @NonNull String password) {
        MutableLiveData<Resource<AuthUser>> result = new MutableLiveData<>();
        result.postValue(Resource.loading());
        String id = employeeIdOrEmail.trim().toUpperCase(Locale.US);
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                SQLiteDatabase db = database.getReadableDatabase();
                try (Cursor c = db.query(HrmsDatabase.TABLE_USERS, null,
                        HrmsDatabase.COL_USER_EMPLOYEE_ID + " = ?", new String[] {id},
                        null, null, null)) {
                    if (c.moveToFirst()
                            && password.equals(c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_USER_PASSWORD)))) {
                        result.postValue(Resource.success(userFromCursor(c)));
                    } else {
                        result.postValue(Resource.error(INVALID_CREDENTIALS));
                    }
                }
            } catch (Exception e) {
                result.postValue(Resource.error("Couldn't sign you in. Please try again."));
            }
        });
        return result;
    }

    @NonNull
    @Override
    public LiveData<Resource<AuthUser>> register(
            @NonNull String employeeId, @NonNull String fullName,
            @NonNull String email, @NonNull String password) {
        MutableLiveData<Resource<AuthUser>> result = new MutableLiveData<>();
        result.postValue(Resource.loading());
        String id = employeeId.trim().toUpperCase(Locale.US);
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                SQLiteDatabase db = database.getWritableDatabase();
                if (exists(db, id)) {
                    result.postValue(Resource.error("An account with this Employee ID already exists."));
                    return;
                }
                String designation = "Employee";
                String department = "General";

                ContentValues user = new ContentValues();
                user.put(HrmsDatabase.COL_USER_EMPLOYEE_ID, id);
                user.put(HrmsDatabase.COL_USER_PASSWORD, password);
                user.put(HrmsDatabase.COL_USER_FULL_NAME, fullName);
                user.put(HrmsDatabase.COL_USER_DESIGNATION, designation);
                user.put(HrmsDatabase.COL_USER_DEPARTMENT, department);
                user.put(HrmsDatabase.COL_USER_ROLE, UserRole.EMPLOYEE.name());
                long rowId = db.insertWithOnConflict(
                        HrmsDatabase.TABLE_USERS, null, user, SQLiteDatabase.CONFLICT_IGNORE);
                if (rowId == -1) {
                    result.postValue(Resource.error("An account with this Employee ID already exists."));
                    return;
                }

                insertProfile(db, id, fullName, email, designation, department);

                result.postValue(Resource.success(
                        new AuthUser(id, fullName, designation, department, UserRole.EMPLOYEE)));
            } catch (Exception e) {
                result.postValue(Resource.error("Couldn't create your account. Please try again."));
            }
        });
        return result;
    }

    private boolean exists(@NonNull SQLiteDatabase db, @NonNull String id) {
        try (Cursor c = db.query(HrmsDatabase.TABLE_USERS,
                new String[] {HrmsDatabase.COL_USER_EMPLOYEE_ID},
                HrmsDatabase.COL_USER_EMPLOYEE_ID + " = ?", new String[] {id},
                null, null, null)) {
            return c.moveToFirst();
        }
    }

    /** Seeds a minimal profile for a new account so the Profile screen isn't empty; editable later. */
    private void insertProfile(@NonNull SQLiteDatabase db, @NonNull String id, @NonNull String fullName,
            @NonNull String email, @NonNull String designation, @NonNull String department) {
        long today = DateUtils.today().getTimeInMillis();
        Calendar dob = DateUtils.calendarFor(1995, 1, 1);
        ContentValues v = new ContentValues();
        v.put(HrmsDatabase.COL_PROFILE_EMPLOYEE_ID, id);
        v.put(HrmsDatabase.COL_PROFILE_FULL_NAME, fullName);
        v.put(HrmsDatabase.COL_PROFILE_DEPARTMENT, department);
        v.put(HrmsDatabase.COL_PROFILE_DESIGNATION, designation);
        v.put(HrmsDatabase.COL_PROFILE_MANAGER, "—");
        v.put(HrmsDatabase.COL_PROFILE_JOINING_DATE, today);
        v.put(HrmsDatabase.COL_PROFILE_EMPLOYMENT_TYPE, "Full-time");
        v.put(HrmsDatabase.COL_PROFILE_DOB, dob.getTimeInMillis());
        v.put(HrmsDatabase.COL_PROFILE_GENDER, "—");
        v.put(HrmsDatabase.COL_PROFILE_PHONE, "—");
        v.put(HrmsDatabase.COL_PROFILE_WORK_EMAIL, email);
        v.put(HrmsDatabase.COL_PROFILE_OFFICE_LOCATION, "—");
        v.put(HrmsDatabase.COL_PROFILE_EMERGENCY_NAME, "—");
        v.put(HrmsDatabase.COL_PROFILE_EMERGENCY_RELATIONSHIP, "—");
        v.put(HrmsDatabase.COL_PROFILE_EMERGENCY_PHONE, "—");
        db.insertWithOnConflict(HrmsDatabase.TABLE_PROFILE, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @NonNull
    private static AuthUser userFromCursor(@NonNull Cursor c) {
        UserRole role = UserRole.valueOf(c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_USER_ROLE)));
        return new AuthUser(
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_USER_EMPLOYEE_ID)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_USER_FULL_NAME)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_USER_DESIGNATION)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_USER_DEPARTMENT)),
                role);
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(SIMULATED_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
