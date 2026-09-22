package com.triotech.hrms.core.network;

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

/**
 * Thin Supabase HTTP client built on {@link HttpURLConnection} — no third-party
 * networking dependency, matching this project's deliberately minimal-deps
 * approach (no Room, no Hilt, no OkHttp).
 *
 * <p>Wraps the two Supabase surfaces the app needs: PostgREST at {@code /rest/v1}
 * for table data and GoTrue at {@code /auth/v1} for authentication. The anon key
 * is sent as the {@code apikey} header on every call; once a user signs in, their
 * access token is sent as the {@code Authorization: Bearer} header so Row Level
 * Security applies. All calls run on a background executor.</p>
 *
 * <p>This is the connection foundation for migrating the mock/DB repositories to
 * Supabase; each repository will call {@link #get}/{@link #post}/{@link #authSignIn}
 * off the main thread. Until the Postgres schema is provisioned, only
 * {@link #checkConnectivity} (which hits GoTrue's health endpoint) will succeed.</p>
 */
public final class SupabaseClient {

    private static final String TAG = "SupabaseClient";
    private static final int TIMEOUT_MS = 15_000;

    private static final String BASE_URL = trimTrailingSlash(BuildConfig.SUPABASE_URL);
    private static final String ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;

    private static volatile SupabaseClient instance;

    private final ExecutorService ioExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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

    /** True only when both the URL and anon key were supplied at build time. */
    public static boolean isConfigured() {
        return !BASE_URL.isEmpty() && !ANON_KEY.isEmpty();
    }

    @NonNull
    public static String getBaseUrl() {
        return BASE_URL;
    }

    // ===================== Public API =====================

    public interface Callback<T> {
        void onResult(@NonNull T result);
    }

    /** GET from PostgREST, e.g. {@code get("expenses?select=*&order=expense_date.desc", token, cb)}. */
    public void get(@NonNull String restPathAndQuery, @Nullable String accessToken,
            @NonNull Callback<ApiResponse> callback) {
        submit(() -> request("GET", BASE_URL + "/rest/v1/" + restPathAndQuery, null, accessToken), callback);
    }

    /** POST JSON to PostgREST (insert). */
    public void post(@NonNull String restPathAndQuery, @NonNull String jsonBody,
            @Nullable String accessToken, @NonNull Callback<ApiResponse> callback) {
        submit(() -> request("POST", BASE_URL + "/rest/v1/" + restPathAndQuery, jsonBody, accessToken), callback);
    }

    /** GoTrue email/password sign-in; the JSON body carries {@code access_token} on success. */
    public void authSignIn(@NonNull String email, @NonNull String password,
            @NonNull Callback<ApiResponse> callback) {
        String body = "{\"email\":" + jsonString(email) + ",\"password\":" + jsonString(password) + "}";
        submit(() -> request("POST", BASE_URL + "/auth/v1/token?grant_type=password", body, null), callback);
    }

    /** Verifies the project is reachable by pinging GoTrue's health endpoint. */
    public void checkConnectivity(@NonNull Callback<Boolean> callback) {
        submit(() -> request("GET", BASE_URL + "/auth/v1/health", null, null),
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

    @NonNull
    private ApiResponse request(
            @NonNull String method, @NonNull String urlString,
            @Nullable String jsonBody, @Nullable String accessToken) {
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
            conn.setRequestProperty("Authorization", "Bearer " + (accessToken != null ? accessToken : ANON_KEY));
            conn.setRequestProperty("Accept", "application/json");
            if (jsonBody != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = conn.getResponseCode();
            String body = readStream(status >= 400 ? conn.getErrorStream() : conn.getInputStream());
            return new ApiResponse(status, body, null);
        } catch (Exception e) {
            Log.w(TAG, "Request failed: " + method + " " + urlString, e);
            return new ApiResponse(0, "", e.getMessage() != null ? e.getMessage() : "Network error");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
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
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.append('"').toString();
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
