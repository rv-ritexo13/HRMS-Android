package com.triotech.hrms.core.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.triotech.hrms.BuildConfig;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/**
 * Thin Supabase HTTP client built on {@link HttpURLConnection} — no third-party
 * networking dependency, matching this project's minimal-deps approach.
 *
 * <p>Owns the signed-in session: it stores the GoTrue access + refresh tokens
 * (persisted so a remembered login survives a restart) and attaches the access
 * token to every PostgREST call so Row Level Security applies. When a call comes
 * back {@code 401} (token expired), it transparently refreshes with the refresh
 * token and retries once — callers never see the expiry.</p>
 *
 * <p>Wraps PostgREST at {@code /rest/v1} for table data and GoTrue at
 * {@code /auth/v1} for auth. {@link #init(Context)} must be called once at
 * startup (from {@code HrmsApplication}) to enable token persistence.</p>
 */
public final class SupabaseClient {

    private static final String TAG = "SupabaseClient";
    private static final int TIMEOUT_MS = 15_000;
    private static final String PREFS = "hrms_supabase_session";
    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";

    private static final String BASE_URL = trimTrailingSlash(BuildConfig.SUPABASE_URL);
    private static final String ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;

    private static volatile SupabaseClient instance;

    private final ExecutorService ioExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable private Context appContext;
    @Nullable private volatile String accessToken;
    @Nullable private volatile String refreshToken;

    private SupabaseClient() {
    }

    @NonNull
    public static SupabaseClient getInstance() {
        if (instance == null) {
            synchronized (SupabaseClient.class) {
                if (instance == null) {
                    instance = new SupabaseClient();
                }
            }
        }
        return instance;
    }

    /** Attaches an app context and restores any persisted session. Call once at startup. */
    public void init(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        SharedPreferences p = prefs();
        if (p != null) {
            accessToken = emptyToNull(p.getString(KEY_ACCESS, null));
            refreshToken = emptyToNull(p.getString(KEY_REFRESH, null));
        }
    }

    public static boolean isConfigured() {
        return !BASE_URL.isEmpty() && !ANON_KEY.isEmpty();
    }

    @NonNull
    public static String getBaseUrl() {
        return BASE_URL;
    }

    public boolean hasSession() {
        return accessToken != null;
    }

    // ===================== Public API =====================

    public interface Callback<T> {
        void onResult(@NonNull T result);
    }

    /** GET from PostgREST, e.g. {@code get("expenses?select=*&order=created_millis.desc", cb)}. */
    public void get(@NonNull String restPathAndQuery, @NonNull Callback<ApiResponse> callback) {
        submit(() -> request("GET", BASE_URL + "/rest/v1/" + restPathAndQuery, null), callback);
    }

    /** POST JSON to PostgREST (insert). */
    public void post(@NonNull String restPathAndQuery, @NonNull String jsonBody, @NonNull Callback<ApiResponse> callback) {
        submit(() -> request("POST", BASE_URL + "/rest/v1/" + restPathAndQuery, jsonBody), callback);
    }

    /** PATCH JSON to PostgREST (update rows matched by the query filter). */
    public void patch(@NonNull String restPathAndQuery, @NonNull String jsonBody, @NonNull Callback<ApiResponse> callback) {
        submit(() -> request("PATCH", BASE_URL + "/rest/v1/" + restPathAndQuery, jsonBody), callback);
    }

    /** GoTrue email/password sign-in; stores the session tokens on success. */
    public void authSignIn(@NonNull String email, @NonNull String password, @NonNull Callback<ApiResponse> callback) {
        String body = "{\"email\":" + jsonString(email) + ",\"password\":" + jsonString(password) + "}";
        submit(() -> {
            ApiResponse r = rawRequest("POST", BASE_URL + "/auth/v1/token?grant_type=password", body, null);
            if (r.isSuccess()) {
                storeTokensFrom(r.body);
            }
            return r;
        }, callback);
    }

    /** Clears the stored session (call on logout). */
    public void clearSession() {
        accessToken = null;
        refreshToken = null;
        SharedPreferences p = prefs();
        if (p != null) {
            p.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).apply();
        }
    }

    /** Verifies the project is reachable by pinging GoTrue's health endpoint. */
    public void checkConnectivity(@NonNull Callback<Boolean> callback) {
        submit(() -> rawRequest("GET", BASE_URL + "/auth/v1/health", null, null),
                response -> callback.onResult(response.isSuccess()));
    }

    // ===================== Internals =====================

    private <T> void submit(@NonNull Producer<T> producer, @NonNull Callback<T> callback) {
        ioExecutor.execute(() -> {
            T result = producer.produce();
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    private interface Producer<T> {
        @NonNull
        T produce();
    }

    /** Authorized request with one transparent token-refresh + retry on 401. */
    @NonNull
    private ApiResponse request(@NonNull String method, @NonNull String url, @Nullable String jsonBody) {
        ApiResponse r = rawRequest(method, url, jsonBody, accessToken);
        if (r.status == 401 && refreshToken != null && refreshBlocking()) {
            r = rawRequest(method, url, jsonBody, accessToken);
        }
        return r;
    }

    /** Exchanges the refresh token for a fresh access token. Returns true on success. */
    private synchronized boolean refreshBlocking() {
        if (refreshToken == null) {
            return false;
        }
        ApiResponse r = rawRequest("POST", BASE_URL + "/auth/v1/token?grant_type=refresh_token",
                "{\"refresh_token\":" + jsonString(refreshToken) + "}", null);
        if (r.isSuccess() && storeTokensFrom(r.body)) {
            return true;
        }
        clearSession(); // refresh token no longer valid — force a fresh login
        return false;
    }

    private boolean storeTokensFrom(@NonNull String body) {
        try {
            JSONObject o = new JSONObject(body);
            String access = emptyToNull(o.optString("access_token", null));
            String refresh = emptyToNull(o.optString("refresh_token", null));
            if (access == null) {
                return false;
            }
            accessToken = access;
            if (refresh != null) {
                refreshToken = refresh;
            }
            SharedPreferences p = prefs();
            if (p != null) {
                p.edit().putString(KEY_ACCESS, accessToken).putString(KEY_REFRESH, refreshToken).apply();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @NonNull
    private ApiResponse rawRequest(
            @NonNull String method, @NonNull String urlString, @Nullable String jsonBody, @Nullable String bearer) {
        if (!isConfigured()) {
            return new ApiResponse(0, "", "Supabase is not configured (missing URL or anon key).");
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("apikey", ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + (bearer != null ? bearer : ANON_KEY));
            conn.setRequestProperty("Accept", "application/json");
            if ("POST".equals(method) || "PATCH".equals(method)) {
                conn.setRequestProperty("Prefer", "return=representation");
            }
            if (jsonBody != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = conn.getResponseCode();
            String respBody = readStream(status >= 400 ? conn.getErrorStream() : conn.getInputStream());
            return new ApiResponse(status, respBody, null);
        } catch (Exception e) {
            Log.w(TAG, "Request failed: " + method + " " + urlString, e);
            return new ApiResponse(0, "", e.getMessage() != null ? e.getMessage() : "Network error");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Nullable
    private SharedPreferences prefs() {
        return appContext == null ? null : appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    private static String readStream(@Nullable InputStream stream) {
        if (stream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception ignored) {
            // Return whatever was read.
        }
        return sb.toString();
    }

    @NonNull
    private static String jsonString(@NonNull String raw) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    @Nullable
    private static String emptyToNull(@Nullable String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    @NonNull
    private static String trimTrailingSlash(@Nullable String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Immutable result of an HTTP call: status code, response body, and an optional local error. */
    public static final class ApiResponse {
        public final int status;
        @NonNull public final String body;
        @Nullable public final String error;

        ApiResponse(int status, @NonNull String body, @Nullable String error) {
            this.status = status;
            this.body = body;
            this.error = error;
        }

        /** 2xx and no local (network) error. */
        public boolean isSuccess() {
            return error == null && status >= 200 && status < 300;
        }
    }
}
