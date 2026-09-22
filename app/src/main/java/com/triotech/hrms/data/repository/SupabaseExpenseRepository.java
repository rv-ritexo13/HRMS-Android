package com.triotech.hrms.data.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.network.SupabaseClient;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.core.util.SessionManager;
import com.triotech.hrms.data.model.Expense;
import com.triotech.hrms.data.model.ExpenseCategory;
import com.triotech.hrms.data.model.ExpenseStatus;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * {@link ExpenseRepository} backed by Supabase PostgREST. Reads/writes the
 * {@code expenses} table under the signed-in user's access token, so Row Level
 * Security limits every call to that employee's own rows. Replaces
 * {@link DbExpenseRepository} with no change to the Expenses UI.
 */
public class SupabaseExpenseRepository implements ExpenseRepository {

    private static final String TABLE = "expenses";

    private final Context appContext;
    private final SupabaseClient client = SupabaseClient.getInstance();

    public SupabaseExpenseRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Nullable
    private String token() {
        return SessionManager.getAccessToken(appContext);
    }

    @NonNull
    @Override
    public LiveData<Resource<List<Expense>>> observeExpenses() {
        MutableLiveData<Resource<List<Expense>>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get(TABLE + "?select=*&order=expense_date_millis.desc", token(), resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load your expenses."));
                return;
            }
            List<Expense> list = parseList(resp.body);
            live.setValue(list.isEmpty() ? Resource.empty() : Resource.success(list));
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<Expense>> observeExpense(@NonNull String id) {
        MutableLiveData<Resource<Expense>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get(TABLE + "?select=*&id=eq." + enc(id), token(), resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load this expense."));
                return;
            }
            List<Expense> list = parseList(resp.body);
            live.setValue(list.isEmpty() ? Resource.error("Expense not found.") : Resource.success(list.get(0)));
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<Expense>> saveExpense(@NonNull Expense expense) {
        MutableLiveData<Resource<Expense>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.post(TABLE, toJson(expense), token(), resp ->
                live.setValue(resp.isSuccess() ? Resource.success(expense)
                        : Resource.error("Couldn't save your expense.")));
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<Expense>> updateExpense(@NonNull Expense expense) {
        MutableLiveData<Resource<Expense>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.patch(TABLE + "?id=eq." + enc(expense.getId()), toJson(expense), token(), resp ->
                live.setValue(resp.isSuccess() ? Resource.success(expense)
                        : Resource.error("Couldn't update your expense.")));
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<Boolean>> submitExpense(@NonNull String id) {
        MutableLiveData<Resource<Boolean>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        String body = "{\"status\":\"" + ExpenseStatus.SUBMITTED.key() + "\"}";
        // Guard so only a DRAFT can be submitted — the filter returns 0 rows otherwise.
        client.patch(TABLE + "?id=eq." + enc(id) + "&status=eq." + ExpenseStatus.DRAFT.key(), body, token(), resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't submit this expense."));
                return;
            }
            boolean changed = parseList(resp.body).size() > 0 || !resp.body.trim().equals("[]");
            live.setValue(changed ? Resource.success(Boolean.TRUE)
                    : Resource.error("This expense can no longer be submitted."));
        });
        return live;
    }

    // ===================== JSON mapping =====================

    @NonNull
    private static List<Expense> parseList(@NonNull String json) {
        List<Expense> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                Expense e = fromJson(arr.getJSONObject(i));
                if (e != null) {
                    out.add(e);
                }
            }
        } catch (Exception ignored) {
            // Return whatever parsed.
        }
        return out;
    }

    @Nullable
    private static Expense fromJson(@NonNull JSONObject o) {
        ExpenseCategory category = ExpenseCategory.fromKey(o.optString("category", null));
        ExpenseStatus status = ExpenseStatus.fromKey(o.optString("status", null));
        if (category == null || status == null) {
            return null;
        }
        String receipt = o.isNull("receipt_name") ? null : o.optString("receipt_name", null);
        return new Expense(
                o.optString("id"),
                o.optString("title"),
                category,
                o.optLong("amount"),
                o.optLong("expense_date_millis"),
                o.optString("description", ""),
                status,
                o.optLong("created_millis"),
                receipt);
    }

    @NonNull
    private static String toJson(@NonNull Expense e) {
        // user_id is intentionally omitted: the column defaults to auth.uid() server-side.
        JSONObject o = new JSONObject();
        try {
            o.put("id", e.getId());
            o.put("title", e.getTitle());
            o.put("category", e.getCategory().key());
            o.put("amount", e.getAmount());
            o.put("expense_date_millis", e.getDateMillis());
            o.put("description", e.getDescription());
            o.put("status", e.getStatus().key());
            o.put("created_millis", e.getCreatedMillis());
            o.put("receipt_name", e.getReceiptName() == null ? JSONObject.NULL : e.getReceiptName());
        } catch (Exception ignored) {
            // JSONObject.put only throws on null keys — not possible here.
        }
        return o.toString();
    }

    @NonNull
    private static String enc(@NonNull String raw) {
        return raw.replace(" ", "%20");
    }
}
