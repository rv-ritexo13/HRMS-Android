package com.triotech.hrms.core.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.triotech.hrms.core.network.SupabaseClient;
import com.triotech.hrms.data.model.AuthUser;
import com.triotech.hrms.data.model.UserRole;

/**
 * Holds the signed-in user's session for Phase 2's mock authentication.
 *
 * <p>"Remember me" is implemented literally: if it was checked at login, the
 * session is written to {@link SharedPreferences} and survives an app restart.
 * If it was left unchecked, the session lives only in the {@code inMemorySession}
 * field below — it works for the rest of this process (navigation, logout, the
 * dashboard reading who's logged in) but a fresh cold start finds no session and
 * returns to Login, same as a real "don't remember me" would behave without a
 * long-lived token. The Employee ID typed at login is remembered separately
 * (regardless of the checkbox's effect on the session itself) purely to prefill
 * the field next time, which is the other half of what users expect "remember
 * me" to do.</p>
 */
public final class SessionManager {

    private static final String PREFS_NAME = "hrms_session_prefs";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_EMPLOYEE_ID = "employee_id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_DESIGNATION = "designation";
    private static final String KEY_DEPARTMENT = "department";
    private static final String KEY_ROLE = "role";
    private static final String KEY_REMEMBERED_ID = "remembered_employee_id";

    @Nullable private static volatile AuthUser inMemorySession;

    private SessionManager() {
    }

    public static void saveSession(@NonNull Context context, @NonNull AuthUser user, boolean rememberMe) {
        inMemorySession = user;
        SharedPreferences.Editor editor = prefs(context).edit();
        if (rememberMe) {
            editor.putBoolean(KEY_LOGGED_IN, true)
                    .putString(KEY_EMPLOYEE_ID, user.getEmployeeId())
                    .putString(KEY_FULL_NAME, user.getFullName())
                    .putString(KEY_DESIGNATION, user.getDesignation())
                    .putString(KEY_DEPARTMENT, user.getDepartment())
                    .putString(KEY_ROLE, user.getRole().name())
                    .putString(KEY_REMEMBERED_ID, user.getEmployeeId());
        } else {
            editor.putBoolean(KEY_LOGGED_IN, false)
                    .remove(KEY_EMPLOYEE_ID)
                    .remove(KEY_FULL_NAME)
                    .remove(KEY_DESIGNATION)
                    .remove(KEY_DEPARTMENT)
                    .remove(KEY_ROLE)
                    .remove(KEY_REMEMBERED_ID);
        }
        editor.apply();
    }

    @Nullable
    public static AuthUser getSession(@NonNull Context context) {
        if (inMemorySession != null) {
            return inMemorySession;
        }
        SharedPreferences prefs = prefs(context);
        if (!prefs.getBoolean(KEY_LOGGED_IN, false)) {
            return null;
        }
        String employeeId = prefs.getString(KEY_EMPLOYEE_ID, null);
        String roleName = prefs.getString(KEY_ROLE, null);
        if (employeeId == null || roleName == null) {
            return null;
        }
        AuthUser user = new AuthUser(
                employeeId,
                prefs.getString(KEY_FULL_NAME, ""),
                prefs.getString(KEY_DESIGNATION, ""),
                prefs.getString(KEY_DEPARTMENT, ""),
                UserRole.valueOf(roleName));
        inMemorySession = user;
        return user;
    }

    public static boolean isLoggedIn(@NonNull Context context) {
        return getSession(context) != null;
    }

    public static void clearSession(@NonNull Context context) {
        inMemorySession = null;
        SupabaseClient.getInstance().clearSession();
        prefs(context).edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .remove(KEY_EMPLOYEE_ID)
                .remove(KEY_FULL_NAME)
                .remove(KEY_DESIGNATION)
                .remove(KEY_DEPARTMENT)
                .remove(KEY_ROLE)
                .apply();
    }

    /** The Employee ID to prefill on the Login screen, or null if none was remembered. */
    @Nullable
    public static String getRememberedEmployeeId(@NonNull Context context) {
        String value = prefs(context).getString(KEY_REMEMBERED_ID, null);
        return (value == null || value.isEmpty()) ? null : value;
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
