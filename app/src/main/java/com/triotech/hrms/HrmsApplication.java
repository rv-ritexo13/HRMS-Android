package com.triotech.hrms;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.util.ThemePreferences;

/**
 * Application entry point. Responsibilities: restore the user's saved
 * light/dark preference before any Activity is created (so there's no theme flash),
 * hand {@link ServiceLocator} an application Context (Phase 3's attendance
 * repository persists today's check-in state to SharedPreferences), and provide
 * a single place to initialize app-wide singletons as the app grows.
 */
public class HrmsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(ThemePreferences.getSavedNightMode(this));
        ServiceLocator.init(this);
    }
}
