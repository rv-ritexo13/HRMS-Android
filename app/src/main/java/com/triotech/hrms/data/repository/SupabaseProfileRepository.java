package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.network.SupabaseClient;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.EmployeeProfile;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * {@link ProfileRepository} backed by Supabase PostgREST. Reads/writes the
 * {@code profiles} table under the signed-in user's token — RLS ensures an
 * employee only ever sees and edits their own profile row. Replaces
 * {@link DbProfileRepository} with no change to the Profile / Edit Profile UI.
 */
public class SupabaseProfileRepository implements ProfileRepository {

    private static final String TABLE = "profiles";

    private final SupabaseClient client = SupabaseClient.getInstance();

    @NonNull
    @Override
    public LiveData<Resource<EmployeeProfile>> observeProfile(@NonNull String employeeId) {
        MutableLiveData<Resource<EmployeeProfile>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get(TABLE + "?select=*&employee_id=eq." + enc(employeeId), resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load your profile."));
                return;
            }
            EmployeeProfile p = firstFrom(resp.body);
            live.setValue(p == null ? Resource.error("Profile not found.") : Resource.success(p));
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<EmployeeProfile>> updateProfile(@NonNull EmployeeProfile profile) {
        MutableLiveData<Resource<EmployeeProfile>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.patch(TABLE + "?employee_id=eq." + enc(profile.getEmployeeId()), toJson(profile), resp ->
                live.setValue(resp.isSuccess() ? Resource.success(profile)
                        : Resource.error("Couldn't save your changes.")));
        return live;
    }

    // ===================== JSON mapping =====================

    @Nullable
    private static EmployeeProfile firstFrom(@NonNull String json) {
        try {
            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0) {
                return null;
            }
            JSONObject o = arr.getJSONObject(0);
            return new EmployeeProfile(
                    o.optString("employee_id", ""),
                    o.optString("department", ""),
                    o.optString("designation", ""),
                    o.optString("reporting_manager", ""),
                    o.optLong("joining_date_millis"),
                    o.optString("employment_type", ""),
                    o.optString("full_name", ""),
                    o.optLong("dob_millis"),
                    o.optString("gender", ""),
                    o.optString("phone", ""),
                    o.optString("work_email", ""),
                    o.optString("office_location", ""),
                    o.optString("emergency_name", ""),
                    o.optString("emergency_relationship", ""),
                    o.optString("emergency_phone", ""));
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    private static String toJson(@NonNull EmployeeProfile p) {
        // employee_id / user_id are the row's identity — not sent in the update body.
        JSONObject o = new JSONObject();
        try {
            o.put("full_name", p.getFullName());
            o.put("department", p.getDepartment());
            o.put("designation", p.getDesignation());
            o.put("reporting_manager", p.getReportingManager());
            o.put("joining_date_millis", p.getJoiningDateMillis());
            o.put("employment_type", p.getEmploymentType());
            o.put("dob_millis", p.getDateOfBirthMillis());
            o.put("gender", p.getGender());
            o.put("phone", p.getPhoneNumber());
            o.put("work_email", p.getWorkEmail());
            o.put("office_location", p.getOfficeLocation());
            o.put("emergency_name", p.getEmergencyName());
            o.put("emergency_relationship", p.getEmergencyRelationship());
            o.put("emergency_phone", p.getEmergencyPhone());
        } catch (Exception ignored) {
            // JSONObject.put only throws on null keys.
        }
        return o.toString();
    }

    @NonNull
    private static String enc(@NonNull String raw) {
        return raw.replace(" ", "%20");
    }
}
