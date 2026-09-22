package com.triotech.hrms.core.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import java.util.List;
import java.util.Locale;

/**
 * Minimal location helpers for attendance check-in — no Google Play Services
 * dependency (keeps the project's minimal-deps approach): reads the last known
 * fix from the framework {@link LocationManager} and reverse-geocodes it. Always
 * permission- and null-safe: if permission isn't granted or no fix is available,
 * callers get {@code null} and check-in proceeds without a location (never blocks).
 */
public final class LocationUtils {

    private LocationUtils() {
    }

    public static boolean hasPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** The most recent fix from any provider, or null if unavailable / no permission. */
    @Nullable
    public static Location lastKnown(@NonNull Context context) {
        if (!hasPermission(context)) {
            return null;
        }
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            return null;
        }
        String[] providers = {
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
        };
        for (String provider : providers) {
            try {
                Location location = lm.getLastKnownLocation(provider);
                if (location != null) {
                    return location;
                }
            } catch (SecurityException | IllegalArgumentException ignored) {
                // Provider missing or permission revoked mid-call — try the next one.
            }
        }
        return null;
    }

    /** A short human-readable place ("Locality, Region") for a coordinate, or null if geocoding fails. */
    @Nullable
    public static String describe(@NonNull Context context, double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> results = geocoder.getFromLocation(lat, lng, 1);
            if (results != null && !results.isEmpty()) {
                Address a = results.get(0);
                String locality = a.getLocality() != null ? a.getLocality() : a.getSubAdminArea();
                String region = a.getAdminArea();
                if (locality != null && region != null) {
                    return locality + ", " + region;
                }
                if (a.getAddressLine(0) != null) {
                    return a.getAddressLine(0);
                }
            }
        } catch (Exception ignored) {
            // Geocoder can throw/return null (no backend, offline) — treat as "no address".
        }
        return null;
    }
}
