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
import com.triotech.hrms.data.model.EmployeeProfile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link ProfileRepository} backed by the local SQLite {@link HrmsDatabase}. All
 * disk work runs on a single-thread executor and results are delivered via
 * {@code LiveData.postValue}, matching {@link DbSalaryRepository}.
 */
public class DbProfileRepository implements ProfileRepository {

    private static final long SIMULATED_LATENCY_MS = 350L;

    private final HrmsDatabase database;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public DbProfileRepository(@NonNull Context context) {
        this.database = HrmsDatabase.getInstance(context);
        ioExecutor.execute(database::getReadableDatabase);
    }

    @NonNull
    @Override
    public LiveData<Resource<EmployeeProfile>> observeProfile(@NonNull String employeeId) {
        MutableLiveData<Resource<EmployeeProfile>> liveData = new MutableLiveData<>();
        liveData.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                EmployeeProfile profile = query(employeeId);
                if (profile == null) {
                    liveData.postValue(Resource.error("Profile not found."));
                } else {
                    liveData.postValue(Resource.success(profile));
                }
            } catch (Exception e) {
                liveData.postValue(Resource.error("Couldn't load your profile."));
            }
        });
        return liveData;
    }

    @NonNull
    @Override
    public LiveData<Resource<EmployeeProfile>> updateProfile(@NonNull EmployeeProfile profile) {
        MutableLiveData<Resource<EmployeeProfile>> liveData = new MutableLiveData<>();
        liveData.postValue(Resource.loading());
        ioExecutor.execute(() -> {
            sleepQuietly();
            try {
                SQLiteDatabase db = database.getWritableDatabase();
                int rows = db.update(HrmsDatabase.TABLE_PROFILE, toValues(profile),
                        HrmsDatabase.COL_PROFILE_EMPLOYEE_ID + " = ?",
                        new String[] {profile.getEmployeeId()});
                if (rows == 0) {
                    liveData.postValue(Resource.error("Couldn't save your changes."));
                } else {
                    liveData.postValue(Resource.success(profile));
                }
            } catch (Exception e) {
                liveData.postValue(Resource.error("Couldn't save your changes."));
            }
        });
        return liveData;
    }

    @Nullable
    private EmployeeProfile query(@NonNull String employeeId) {
        SQLiteDatabase db = database.getReadableDatabase();
        try (Cursor c = db.query(HrmsDatabase.TABLE_PROFILE, null,
                HrmsDatabase.COL_PROFILE_EMPLOYEE_ID + " = ?", new String[] {employeeId},
                null, null, null)) {
            if (c.moveToFirst()) {
                return fromCursor(c);
            }
        }
        return null;
    }

    @NonNull
    private static ContentValues toValues(@NonNull EmployeeProfile p) {
        ContentValues v = new ContentValues();
        v.put(HrmsDatabase.COL_PROFILE_EMPLOYEE_ID, p.getEmployeeId());
        v.put(HrmsDatabase.COL_PROFILE_FULL_NAME, p.getFullName());
        v.put(HrmsDatabase.COL_PROFILE_DEPARTMENT, p.getDepartment());
        v.put(HrmsDatabase.COL_PROFILE_DESIGNATION, p.getDesignation());
        v.put(HrmsDatabase.COL_PROFILE_MANAGER, p.getReportingManager());
        v.put(HrmsDatabase.COL_PROFILE_JOINING_DATE, p.getJoiningDateMillis());
        v.put(HrmsDatabase.COL_PROFILE_EMPLOYMENT_TYPE, p.getEmploymentType());
        v.put(HrmsDatabase.COL_PROFILE_DOB, p.getDateOfBirthMillis());
        v.put(HrmsDatabase.COL_PROFILE_GENDER, p.getGender());
        v.put(HrmsDatabase.COL_PROFILE_PHONE, p.getPhoneNumber());
        v.put(HrmsDatabase.COL_PROFILE_WORK_EMAIL, p.getWorkEmail());
        v.put(HrmsDatabase.COL_PROFILE_OFFICE_LOCATION, p.getOfficeLocation());
        v.put(HrmsDatabase.COL_PROFILE_EMERGENCY_NAME, p.getEmergencyName());
        v.put(HrmsDatabase.COL_PROFILE_EMERGENCY_RELATIONSHIP, p.getEmergencyRelationship());
        v.put(HrmsDatabase.COL_PROFILE_EMERGENCY_PHONE, p.getEmergencyPhone());
        return v;
    }

    @NonNull
    private static EmployeeProfile fromCursor(@NonNull Cursor c) {
        return new EmployeeProfile(
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_EMPLOYEE_ID)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_DEPARTMENT)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_DESIGNATION)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_MANAGER)),
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_JOINING_DATE)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_EMPLOYMENT_TYPE)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_FULL_NAME)),
                c.getLong(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_DOB)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_GENDER)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_PHONE)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_WORK_EMAIL)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_OFFICE_LOCATION)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_EMERGENCY_NAME)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_EMERGENCY_RELATIONSHIP)),
                c.getString(c.getColumnIndexOrThrow(HrmsDatabase.COL_PROFILE_EMERGENCY_PHONE)));
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(SIMULATED_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
