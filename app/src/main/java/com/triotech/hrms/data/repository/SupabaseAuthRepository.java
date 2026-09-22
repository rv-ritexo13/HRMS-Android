package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.network.SupabaseClient;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.AuthUser;
import com.triotech.hrms.data.model.UserRole;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * {@link AuthRepository} backed by Supabase Auth (GoTrue). Login maps an Employee
 * ID to its Supabase email ({@code emp001 -> emp001@hrms.app}) unless an email is
 * typed directly, signs in for an access token (stored in {@link SessionManager}
 * so PostgREST calls run under Row Level Security), then loads the matching
 * profile row for the display name / department.
 *
 * <p>Account creation is intentionally admin-managed (needs the service_role key,
 * which must stay server-side), so {@link #register} returns a directed error.</p>
 */
public class SupabaseAuthRepository implements AuthRepository {

    private static final String INVALID = "Invalid Employee ID or password";

    private final SupabaseClient client = SupabaseClient.getInstance();

    @NonNull
    @Override
    public LiveData<Resource<AuthUser>> login(@NonNull String employeeIdOrEmail, @NonNull String password) {
        MutableLiveData<Resource<AuthUser>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        String input = employeeIdOrEmail.trim();
        String email = input.contains("@") ? input : input.toLowerCase(Locale.US) + "@hrms.app";
        final String employeeId = input.contains("@") ? input : input.toUpperCase(Locale.US);

        client.authSignIn(email, password, resp -> {
            if (!resp.isSuccess()) {
                result.setValue(Resource.error(INVALID));
                return;
            }
            try {
                // The client has already stored the access + refresh tokens from a
                // successful sign-in, so PostgREST calls below are authorized.
                JSONObject body = new JSONObject(resp.body);
                String metaName = body.optJSONObject("user") != null
                        && body.getJSONObject("user").optJSONObject("user_metadata") != null
                        ? body.getJSONObject("user").getJSONObject("user_metadata").optString("name", "")
                        : "";
                loadProfile(employeeId, metaName, result);
            } catch (Exception e) {
                result.setValue(Resource.error("Couldn't sign you in. Please try again."));
            }
        });
        return result;
    }

    /** Fetches the signed-in user's profile row to fill name/department/designation. */
    private void loadProfile(@NonNull String employeeId, @NonNull String fallbackName,
            @NonNull MutableLiveData<Resource<AuthUser>> result) {
        client.get("profiles?select=employee_id,full_name,department,designation&limit=1", resp -> {
            String id = employeeId;
            String name = fallbackName.isEmpty() ? employeeId : fallbackName;
            String department = "General";
            String designation = "Employee";
            try {
                if (resp.isSuccess()) {
                    JSONArray arr = new JSONArray(resp.body);
                    if (arr.length() > 0) {
                        JSONObject p = arr.getJSONObject(0);
                        id = p.optString("employee_id", employeeId);
                        name = p.optString("full_name", name);
                        department = p.optString("department", department);
                        designation = p.optString("designation", designation);
                    }
                }
            } catch (Exception ignored) {
                // Fall back to the values above.
            }
            result.setValue(Resource.success(new AuthUser(id, name, designation, department, UserRole.EMPLOYEE)));
        });
    }

    @NonNull
    @Override
    public LiveData<Resource<AuthUser>> register(
            @NonNull String employeeId, @NonNull String fullName,
            @NonNull String email, @NonNull String password) {
        MutableLiveData<Resource<AuthUser>> result = new MutableLiveData<>();
        result.setValue(Resource.error(
                "New accounts are created by your HR administrator. Please contact them to get access."));
        return result;
    }
}
