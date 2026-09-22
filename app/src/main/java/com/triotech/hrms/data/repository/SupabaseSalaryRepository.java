package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.network.SupabaseClient;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Payslip;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * {@link SalaryRepository} backed by Supabase PostgREST. Reads the
 * {@code payslips} table under the signed-in user's token (RLS-scoped to their
 * own months). Read-only, matching the Salary screen. Replaces
 * {@link DbSalaryRepository} with no change to the UI.
 */
public class SupabaseSalaryRepository implements SalaryRepository {

    private static final String TABLE = "payslips";

    private final SupabaseClient client = SupabaseClient.getInstance();

    @NonNull
    @Override
    public LiveData<Resource<List<Payslip>>> observeSalaryHistory() {
        MutableLiveData<Resource<List<Payslip>>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get(TABLE + "?select=*&order=month_key.desc", resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load your salary history."));
                return;
            }
            List<Payslip> list = parseList(resp.body);
            live.setValue(list.isEmpty() ? Resource.empty() : Resource.success(list));
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<Payslip>> observePayslip(@NonNull String monthKey) {
        MutableLiveData<Resource<Payslip>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get(TABLE + "?select=*&month_key=eq." + enc(monthKey), resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load this payslip."));
                return;
            }
            List<Payslip> list = parseList(resp.body);
            live.setValue(list.isEmpty() ? Resource.error("Payslip not found.") : Resource.success(list.get(0)));
        });
        return live;
    }

    // ===================== JSON mapping =====================

    @NonNull
    private static List<Payslip> parseList(@NonNull String json) {
        List<Payslip> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                Payslip p = fromJson(arr.getJSONObject(i));
                if (p != null) {
                    out.add(p);
                }
            }
        } catch (Exception ignored) {
            // Return whatever parsed.
        }
        return out;
    }

    @Nullable
    private static Payslip fromJson(@NonNull JSONObject o) {
        String monthKey = o.optString("month_key", null);
        if (monthKey == null) {
            return null;
        }
        return new Payslip(
                monthKey,
                o.optInt("year"),
                o.optInt("month"),
                o.optString("employee_id", ""),
                o.optString("employee_name", ""),
                o.optString("department", ""),
                o.optString("designation", ""),
                o.optLong("basic"),
                o.optLong("hra"),
                o.optLong("special_allowance"),
                o.optLong("other_allowances"),
                o.optLong("provident_fund"),
                o.optLong("professional_tax"),
                o.optLong("other_deductions"),
                o.optLong("credit_date_millis"),
                o.optInt("credited", 1) != 0);
    }

    @NonNull
    private static String enc(@NonNull String raw) {
        return raw.replace(" ", "%20");
    }
}
