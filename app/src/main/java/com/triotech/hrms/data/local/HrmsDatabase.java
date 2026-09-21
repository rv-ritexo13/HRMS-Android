package com.triotech.hrms.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.triotech.hrms.core.util.DateUtils;
import java.util.Calendar;

/**
 * The app's local SQLite store. Introduced in Phase 5 so salary/payslip data is
 * persisted in a real, inspectable database (Android Studio's Database Inspector
 * can open {@code hrms.db}) rather than living only in memory.
 *
 * <p>Deliberately built on the framework {@link SQLiteOpenHelper} instead of Room:
 * the project is Java-only with no annotation processors wired up, and a single
 * seeded table doesn't justify pulling in a code-gen dependency. The schema is
 * versioned, so migrating to Room later is a contained change.</p>
 *
 * <p>On first creation the {@code payslips} table is seeded with a realistic
 * salary history for the demo employee (EMP001), so the Salary screen looks
 * complete the moment the app is installed.</p>
 */
public final class HrmsDatabase extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "hrms.db";
    private static final int DATABASE_VERSION = 4;

    public static final String TABLE_PAYSLIPS = "payslips";
    public static final String TABLE_PROFILE = "profile";
    public static final String TABLE_LEAVE_BALANCES = "leave_balances";
    public static final String TABLE_LEAVE_REQUESTS = "leave_requests";
    public static final String TABLE_USERS = "users";

    // users columns
    public static final String COL_USER_EMPLOYEE_ID = "employee_id";
    public static final String COL_USER_PASSWORD = "password";
    public static final String COL_USER_FULL_NAME = "full_name";
    public static final String COL_USER_DESIGNATION = "designation";
    public static final String COL_USER_DEPARTMENT = "department";
    public static final String COL_USER_ROLE = "role";

    // leave_balances columns
    public static final String COL_LB_TYPE = "leave_type";
    public static final String COL_LB_TOTAL = "total_days";
    public static final String COL_LB_USED = "used_days";

    // leave_requests columns
    public static final String COL_LR_ID = "id";
    public static final String COL_LR_TYPE = "leave_type";
    public static final String COL_LR_START = "start_millis";
    public static final String COL_LR_END = "end_millis";
    public static final String COL_LR_DAYS = "days";
    public static final String COL_LR_REASON = "reason";
    public static final String COL_LR_STATUS = "status";
    public static final String COL_LR_APPLIED = "applied_millis";
    public static final String COL_LR_MANAGER_COMMENTS = "manager_comments";
    public static final String COL_LR_ATTACHMENT = "attachment_name";

    // profile columns
    public static final String COL_PROFILE_EMPLOYEE_ID = "employee_id";
    public static final String COL_PROFILE_FULL_NAME = "full_name";
    public static final String COL_PROFILE_DEPARTMENT = "department";
    public static final String COL_PROFILE_DESIGNATION = "designation";
    public static final String COL_PROFILE_MANAGER = "reporting_manager";
    public static final String COL_PROFILE_JOINING_DATE = "joining_date_millis";
    public static final String COL_PROFILE_EMPLOYMENT_TYPE = "employment_type";
    public static final String COL_PROFILE_DOB = "dob_millis";
    public static final String COL_PROFILE_GENDER = "gender";
    public static final String COL_PROFILE_PHONE = "phone";
    public static final String COL_PROFILE_WORK_EMAIL = "work_email";
    public static final String COL_PROFILE_OFFICE_LOCATION = "office_location";
    public static final String COL_PROFILE_EMERGENCY_NAME = "emergency_name";
    public static final String COL_PROFILE_EMERGENCY_RELATIONSHIP = "emergency_relationship";
    public static final String COL_PROFILE_EMERGENCY_PHONE = "emergency_phone";

    public static final String COL_MONTH_KEY = "month_key";
    public static final String COL_YEAR = "year";
    public static final String COL_MONTH = "month";
    public static final String COL_EMPLOYEE_ID = "employee_id";
    public static final String COL_EMPLOYEE_NAME = "employee_name";
    public static final String COL_DEPARTMENT = "department";
    public static final String COL_DESIGNATION = "designation";
    public static final String COL_BASIC = "basic";
    public static final String COL_HRA = "hra";
    public static final String COL_SPECIAL_ALLOWANCE = "special_allowance";
    public static final String COL_OTHER_ALLOWANCES = "other_allowances";
    public static final String COL_PROVIDENT_FUND = "provident_fund";
    public static final String COL_PROFESSIONAL_TAX = "professional_tax";
    public static final String COL_OTHER_DEDUCTIONS = "other_deductions";
    public static final String COL_CREDIT_DATE = "credit_date_millis";
    public static final String COL_CREDITED = "credited";

    // Demo employee — kept consistent with FakeAuthRepository's EMP001 account.
    private static final String DEMO_EMPLOYEE_ID = "EMP001";
    private static final String DEMO_EMPLOYEE_NAME = "Rahul Verma";
    private static final String DEMO_DEPARTMENT = "Engineering";
    private static final String DEMO_DESIGNATION = "Software Engineer";

    // Fixed monthly deductions for the demo (whole rupees): PF + Professional Tax + Other = 6,000.
    private static final long PF = 5_100L;
    private static final long PROFESSIONAL_TAX = 200L;
    private static final long OTHER_DEDUCTIONS = 700L;

    private static volatile HrmsDatabase instance;

    private HrmsDatabase(@NonNull Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @NonNull
    public static HrmsDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (HrmsDatabase.class) {
                if (instance == null) {
                    instance = new HrmsDatabase(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PAYSLIPS + " ("
                + COL_MONTH_KEY + " TEXT PRIMARY KEY, "
                + COL_YEAR + " INTEGER NOT NULL, "
                + COL_MONTH + " INTEGER NOT NULL, "
                + COL_EMPLOYEE_ID + " TEXT NOT NULL, "
                + COL_EMPLOYEE_NAME + " TEXT NOT NULL, "
                + COL_DEPARTMENT + " TEXT NOT NULL, "
                + COL_DESIGNATION + " TEXT NOT NULL, "
                + COL_BASIC + " INTEGER NOT NULL, "
                + COL_HRA + " INTEGER NOT NULL, "
                + COL_SPECIAL_ALLOWANCE + " INTEGER NOT NULL, "
                + COL_OTHER_ALLOWANCES + " INTEGER NOT NULL, "
                + COL_PROVIDENT_FUND + " INTEGER NOT NULL, "
                + COL_PROFESSIONAL_TAX + " INTEGER NOT NULL, "
                + COL_OTHER_DEDUCTIONS + " INTEGER NOT NULL, "
                + COL_CREDIT_DATE + " INTEGER NOT NULL, "
                + COL_CREDITED + " INTEGER NOT NULL"
                + ")");
        seedPayslips(db);

        db.execSQL("CREATE TABLE " + TABLE_PROFILE + " ("
                + COL_PROFILE_EMPLOYEE_ID + " TEXT PRIMARY KEY, "
                + COL_PROFILE_FULL_NAME + " TEXT NOT NULL, "
                + COL_PROFILE_DEPARTMENT + " TEXT NOT NULL, "
                + COL_PROFILE_DESIGNATION + " TEXT NOT NULL, "
                + COL_PROFILE_MANAGER + " TEXT NOT NULL, "
                + COL_PROFILE_JOINING_DATE + " INTEGER NOT NULL, "
                + COL_PROFILE_EMPLOYMENT_TYPE + " TEXT NOT NULL, "
                + COL_PROFILE_DOB + " INTEGER NOT NULL, "
                + COL_PROFILE_GENDER + " TEXT NOT NULL, "
                + COL_PROFILE_PHONE + " TEXT NOT NULL, "
                + COL_PROFILE_WORK_EMAIL + " TEXT NOT NULL, "
                + COL_PROFILE_OFFICE_LOCATION + " TEXT NOT NULL, "
                + COL_PROFILE_EMERGENCY_NAME + " TEXT NOT NULL, "
                + COL_PROFILE_EMERGENCY_RELATIONSHIP + " TEXT NOT NULL, "
                + COL_PROFILE_EMERGENCY_PHONE + " TEXT NOT NULL"
                + ")");
        seedProfile(db);

        db.execSQL("CREATE TABLE " + TABLE_LEAVE_BALANCES + " ("
                + COL_LB_TYPE + " TEXT PRIMARY KEY, "
                + COL_LB_TOTAL + " INTEGER NOT NULL, "
                + COL_LB_USED + " INTEGER NOT NULL"
                + ")");
        seedLeaveBalances(db);

        db.execSQL("CREATE TABLE " + TABLE_LEAVE_REQUESTS + " ("
                + COL_LR_ID + " TEXT PRIMARY KEY, "
                + COL_LR_TYPE + " TEXT NOT NULL, "
                + COL_LR_START + " INTEGER NOT NULL, "
                + COL_LR_END + " INTEGER NOT NULL, "
                + COL_LR_DAYS + " INTEGER NOT NULL, "
                + COL_LR_REASON + " TEXT NOT NULL, "
                + COL_LR_STATUS + " TEXT NOT NULL, "
                + COL_LR_APPLIED + " INTEGER NOT NULL, "
                + COL_LR_MANAGER_COMMENTS + " TEXT NOT NULL, "
                + COL_LR_ATTACHMENT + " TEXT"
                + ")");
        seedLeaveRequests(db);

        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_EMPLOYEE_ID + " TEXT PRIMARY KEY, "
                + COL_USER_PASSWORD + " TEXT NOT NULL, "
                + COL_USER_FULL_NAME + " TEXT NOT NULL, "
                + COL_USER_DESIGNATION + " TEXT NOT NULL, "
                + COL_USER_DEPARTMENT + " TEXT NOT NULL, "
                + COL_USER_ROLE + " TEXT NOT NULL"
                + ")");
        seedUsers(db);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        // On a schema bump, rebuild the seeded tables.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAYSLIPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROFILE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEAVE_BALANCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEAVE_REQUESTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    private void seedUsers(@NonNull SQLiteDatabase db) {
        insertUser(db, DEMO_EMPLOYEE_ID, "123456", DEMO_EMPLOYEE_NAME, DEMO_DESIGNATION, DEMO_DEPARTMENT, "EMPLOYEE");
        insertUser(db, "ADMIN001", "123456", "Anita Kapoor", "HR Administrator", "People Ops", "ADMIN");
    }

    private void insertUser(@NonNull SQLiteDatabase db, @NonNull String id, @NonNull String password,
            @NonNull String name, @NonNull String designation, @NonNull String department, @NonNull String role) {
        ContentValues v = new ContentValues();
        v.put(COL_USER_EMPLOYEE_ID, id);
        v.put(COL_USER_PASSWORD, password);
        v.put(COL_USER_FULL_NAME, name);
        v.put(COL_USER_DESIGNATION, designation);
        v.put(COL_USER_DEPARTMENT, department);
        v.put(COL_USER_ROLE, role);
        db.insert(TABLE_USERS, null, v);
    }

    private void seedLeaveBalances(@NonNull SQLiteDatabase db) {
        insertBalance(db, "CASUAL", 12, 4);            // available 8
        insertBalance(db, "SICK", 12, 2);              // available 10
        insertBalance(db, "EARNED", 18, 4);            // available 14
        insertBalance(db, "WORK_FROM_HOME", 8, 3);     // available 5
        insertBalance(db, "OPTIONAL_HOLIDAY", 4, 1);   // available 3
    }

    private void insertBalance(@NonNull SQLiteDatabase db, @NonNull String type, int total, int used) {
        ContentValues v = new ContentValues();
        v.put(COL_LB_TYPE, type);
        v.put(COL_LB_TOTAL, total);
        v.put(COL_LB_USED, used);
        db.insert(TABLE_LEAVE_BALANCES, null, v);
    }

    private void seedLeaveRequests(@NonNull SQLiteDatabase db) {
        insertRequest(db, "LR-seed-1", "EARNED",
                date(2026, 8, 10), date(2026, 8, 12), 3, "Family function at hometown.",
                "APPROVED", date(2026, 8, 1), "Approved. Enjoy your time off!", null);
        insertRequest(db, "LR-seed-2", "SICK",
                date(2026, 9, 5), date(2026, 9, 5), 1, "Down with fever.",
                "APPROVED", date(2026, 9, 5), "Get well soon.", null);
        insertRequest(db, "LR-seed-3", "CASUAL",
                date(2026, 9, 22), date(2026, 9, 23), 2, "Personal work.",
                "PENDING", date(2026, 9, 18), "", null);
        insertRequest(db, "LR-seed-4", "WORK_FROM_HOME",
                date(2026, 7, 15), date(2026, 7, 15), 1, "Home maintenance visit.",
                "REJECTED", date(2026, 7, 14), "Team on-site day; please reschedule.", null);
        insertRequest(db, "LR-seed-5", "CASUAL",
                date(2026, 6, 2), date(2026, 6, 2), 1, "Ran an errand, no longer needed.",
                "CANCELLED", date(2026, 6, 1), "", null);
    }

    private void insertRequest(
            @NonNull SQLiteDatabase db, @NonNull String id, @NonNull String type,
            long start, long end, int days, @NonNull String reason,
            @NonNull String status, long applied, @NonNull String managerComments,
            @Nullable String attachment) {
        ContentValues v = new ContentValues();
        v.put(COL_LR_ID, id);
        v.put(COL_LR_TYPE, type);
        v.put(COL_LR_START, start);
        v.put(COL_LR_END, end);
        v.put(COL_LR_DAYS, days);
        v.put(COL_LR_REASON, reason);
        v.put(COL_LR_STATUS, status);
        v.put(COL_LR_APPLIED, applied);
        v.put(COL_LR_MANAGER_COMMENTS, managerComments);
        v.put(COL_LR_ATTACHMENT, attachment);
        db.insert(TABLE_LEAVE_REQUESTS, null, v);
    }

    private static long date(int year, int month, int day) {
        return DateUtils.calendarFor(year, month, day).getTimeInMillis();
    }

    private void seedProfile(@NonNull SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COL_PROFILE_EMPLOYEE_ID, DEMO_EMPLOYEE_ID);
        values.put(COL_PROFILE_FULL_NAME, DEMO_EMPLOYEE_NAME);
        values.put(COL_PROFILE_DEPARTMENT, DEMO_DEPARTMENT);
        values.put(COL_PROFILE_DESIGNATION, DEMO_DESIGNATION);
        values.put(COL_PROFILE_MANAGER, "Vikram Rao");
        values.put(COL_PROFILE_JOINING_DATE, DateUtils.calendarFor(2022, 6, 13).getTimeInMillis());
        values.put(COL_PROFILE_EMPLOYMENT_TYPE, "Full-time");
        values.put(COL_PROFILE_DOB, DateUtils.calendarFor(1996, 3, 8).getTimeInMillis());
        values.put(COL_PROFILE_GENDER, "Male");
        values.put(COL_PROFILE_PHONE, "+91 98765 43210");
        values.put(COL_PROFILE_WORK_EMAIL, "rahul.verma@riteox.co.in");
        values.put(COL_PROFILE_OFFICE_LOCATION, "Bengaluru - Prestige Tech Park");
        values.put(COL_PROFILE_EMERGENCY_NAME, "Sunita Verma");
        values.put(COL_PROFILE_EMERGENCY_RELATIONSHIP, "Mother");
        values.put(COL_PROFILE_EMERGENCY_PHONE, "+91 98111 22334");
        db.insert(TABLE_PROFILE, null, values);
    }

    /**
     * Eight months of history ending September 2026, matching the Phase 5 spec
     * (Sep/Aug ₹79,000 net, Jul/Jun ₹77,500 net, ...). Gross varies by month;
     * deductions are held constant at ₹6,000 so net = gross − 6,000.
     */
    private void seedPayslips(@NonNull SQLiteDatabase db) {
        long[] grossByMonthDesc = {
                85_000L, // 2026-09
                85_000L, // 2026-08
                83_500L, // 2026-07
                83_500L, // 2026-06
                83_500L, // 2026-05
                82_000L, // 2026-04
                82_000L, // 2026-03
                80_500L  // 2026-02
        };

        int year = 2026;
        int month = 9;
        for (long gross : grossByMonthDesc) {
            db.insert(TABLE_PAYSLIPS, null, buildRow(year, month, gross));
            month--;
            if (month == 0) {
                month = 12;
                year--;
            }
        }
    }

    @NonNull
    private ContentValues buildRow(int year, int month, long gross) {
        // Split gross into components with clean, realistic proportions.
        long basic = Math.round(gross * 0.50);
        long hra = Math.round(gross * 0.20);
        long special = Math.round(gross * 0.25);
        long other = gross - basic - hra - special; // remainder keeps the sum exact

        // Salary credited on the last day of the month.
        Calendar credit = DateUtils.calendarFor(year, month, 1);
        credit.set(Calendar.DAY_OF_MONTH, credit.getActualMaximum(Calendar.DAY_OF_MONTH));

        ContentValues values = new ContentValues();
        values.put(COL_MONTH_KEY, String.format(java.util.Locale.US, "%04d-%02d", year, month));
        values.put(COL_YEAR, year);
        values.put(COL_MONTH, month);
        values.put(COL_EMPLOYEE_ID, DEMO_EMPLOYEE_ID);
        values.put(COL_EMPLOYEE_NAME, DEMO_EMPLOYEE_NAME);
        values.put(COL_DEPARTMENT, DEMO_DEPARTMENT);
        values.put(COL_DESIGNATION, DEMO_DESIGNATION);
        values.put(COL_BASIC, basic);
        values.put(COL_HRA, hra);
        values.put(COL_SPECIAL_ALLOWANCE, special);
        values.put(COL_OTHER_ALLOWANCES, other);
        values.put(COL_PROVIDENT_FUND, PF);
        values.put(COL_PROFESSIONAL_TAX, PROFESSIONAL_TAX);
        values.put(COL_OTHER_DEDUCTIONS, OTHER_DEDUCTIONS);
        values.put(COL_CREDIT_DATE, credit.getTimeInMillis());
        values.put(COL_CREDITED, 1);
        return values;
    }
}
