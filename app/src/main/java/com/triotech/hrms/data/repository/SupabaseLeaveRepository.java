package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.network.SupabaseClient;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.LeaveBalance;
import com.triotech.hrms.data.model.LeaveRequest;
import com.triotech.hrms.data.model.LeaveStatus;
import com.triotech.hrms.data.model.LeaveType;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * {@link LeaveRepository} backed by Supabase PostgREST. Reads/writes the
 * {@code leave_balances} and {@code leave_requests} tables under the signed-in
 * user's token, so Row Level Security limits every call to that employee's rows.
 * Replaces {@link DbLeaveRepository} with no change to the Leave UI.
 */
public class SupabaseLeaveRepository implements LeaveRepository {

    private static final String T_BALANCES = "leave_balances";
    private static final String T_REQUESTS = "leave_requests";

    private final SupabaseClient client = SupabaseClient.getInstance();

    @NonNull
    @Override
    public LiveData<Resource<List<LeaveBalance>>> observeBalances() {
        MutableLiveData<Resource<List<LeaveBalance>>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get(T_BALANCES + "?select=*", resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load your leave balances."));
                return;
            }
            List<LeaveBalance> list = parseBalances(resp.body);
            live.setValue(list.isEmpty() ? Resource.empty() : Resource.success(list));
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<List<LeaveRequest>>> observeRequests() {
        MutableLiveData<Resource<List<LeaveRequest>>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get(T_REQUESTS + "?select=*&order=applied_millis.desc", resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load your leave requests."));
                return;
            }
            List<LeaveRequest> list = parseRequests(resp.body);
            live.setValue(list.isEmpty() ? Resource.empty() : Resource.success(list));
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<LeaveRequest>> observeRequest(@NonNull String id) {
        MutableLiveData<Resource<LeaveRequest>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get(T_REQUESTS + "?select=*&id=eq." + enc(id), resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load this request."));
                return;
            }
            List<LeaveRequest> list = parseRequests(resp.body);
            live.setValue(list.isEmpty() ? Resource.error("Request not found.") : Resource.success(list.get(0)));
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<LeaveRequest>> applyLeave(@NonNull LeaveRequest draft) {
        MutableLiveData<Resource<LeaveRequest>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.post(T_REQUESTS, toJson(draft), resp ->
                live.setValue(resp.isSuccess() ? Resource.success(draft)
                        : Resource.error("Couldn't submit your leave request.")));
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<Boolean>> cancelRequest(@NonNull String id) {
        MutableLiveData<Resource<Boolean>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        String body = "{\"status\":\"" + LeaveStatus.CANCELLED.key() + "\"}";
        // Only a PENDING request can be cancelled — the filter matches 0 rows otherwise.
        client.patch(T_REQUESTS + "?id=eq." + enc(id) + "&status=eq." + LeaveStatus.PENDING.key(), body, resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't cancel this request."));
                return;
            }
            boolean changed = !resp.body.trim().equals("[]");
            live.setValue(changed ? Resource.success(Boolean.TRUE)
                    : Resource.error("This request can no longer be cancelled."));
        });
        return live;
    }

    // ===================== JSON mapping =====================

    @NonNull
    private static List<LeaveBalance> parseBalances(@NonNull String json) {
        List<LeaveBalance> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                LeaveType type = LeaveType.fromKey(o.optString("leave_type", null));
                if (type != null) {
                    out.add(new LeaveBalance(type, o.optInt("total_days"), o.optInt("used_days")));
                }
            }
        } catch (Exception ignored) {
            // Return whatever parsed.
        }
        return out;
    }

    @NonNull
    private static List<LeaveRequest> parseRequests(@NonNull String json) {
        List<LeaveRequest> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                LeaveRequest r = requestFromJson(arr.getJSONObject(i));
                if (r != null) {
                    out.add(r);
                }
            }
        } catch (Exception ignored) {
            // Return whatever parsed.
        }
        return out;
    }

    @Nullable
    private static LeaveRequest requestFromJson(@NonNull JSONObject o) {
        LeaveType type = LeaveType.fromKey(o.optString("leave_type", null));
        LeaveStatus status = LeaveStatus.fromKey(o.optString("status", null));
        if (type == null || status == null) {
            return null;
        }
        String attachment = o.isNull("attachment_name") ? null : o.optString("attachment_name", null);
        return new LeaveRequest(
                o.optString("id"),
                type,
                o.optLong("start_millis"),
                o.optLong("end_millis"),
                o.optInt("days"),
                o.optString("reason", ""),
                status,
                o.optLong("applied_millis"),
                o.optString("manager_comments", ""),
                attachment);
    }

    @NonNull
    private static String toJson(@NonNull LeaveRequest r) {
        // user_id is omitted: the column defaults to auth.uid() server-side.
        JSONObject o = new JSONObject();
        try {
            o.put("id", r.getId());
            o.put("leave_type", r.getType().key());
            o.put("start_millis", r.getStartMillis());
            o.put("end_millis", r.getEndMillis());
            o.put("days", r.getDays());
            o.put("reason", r.getReason());
            o.put("status", r.getStatus().key());
            o.put("applied_millis", r.getAppliedMillis());
            o.put("manager_comments", r.getManagerComments());
            o.put("attachment_name", r.getAttachmentName() == null ? JSONObject.NULL : r.getAttachmentName());
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
