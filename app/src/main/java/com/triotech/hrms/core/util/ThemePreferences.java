package com.triotech.hrms.core.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Persists the user's light/dark mode choice from the Profile screen across app
 * restarts. Backed by SharedPreferences for now; Phase 2 can move this to a
 * proper Settings/DataStore-backed repository without changing the call sites.
 */
public final class ThemePreferences {

    private static final String PREFS_NAME = "hrms_theme_prefs";
    private static final String KEY_NIGHT_MODE = "key_night_mode";

    private ThemePreferences() {
    }

    public static int getSavedNightMode(@NonNull Context context) {
        return prefs(context).getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setNightMode(@NonNull Context context, int mode) {
        prefs(context).edit().putInt(KEY_NIGHT_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static boolean isDarkModeActive(@NonNull Context context) {
        int current = getSavedNightMode(context);
        if (current == AppCompatDelegate.MODE_NIGHT_YES) {
            return true;
        }
        if (current == AppCompatDelegate.MODE_NIGHT_NO) {
            return false;
        }
        int uiMode = context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
