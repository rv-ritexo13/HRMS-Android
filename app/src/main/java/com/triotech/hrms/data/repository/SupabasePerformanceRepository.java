package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.network.SupabaseClient;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Goal;
import com.triotech.hrms.data.model.Kra;
import com.triotech.hrms.data.model.PerformanceOverview;
import com.triotech.hrms.data.model.PerformanceReview;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * {@link PerformanceRepository} backed by Supabase PostgREST. Loads the three
 * performance tables ({@code performance_reviews}, {@code goals}, {@code kras})
 * under the signed-in token (RLS-scoped) and assembles them into one
 * {@link PerformanceOverview}. Replaces {@link MockPerformanceRepository}.
 */
public class SupabasePerformanceRepository implements PerformanceRepository {

    private final SupabaseClient client = SupabaseClient.getInstance();

    @NonNull
    @Override
    public LiveData<Resource<PerformanceOverview>> observePerformance() {
        MutableLiveData<Resource<PerformanceOverview>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());

        // Callbacks are delivered on the main thread, so this coordination is race-free.
        final List<PerformanceReview> reviews = new ArrayList<>();
        final List<Goal> goals = new ArrayList<>();
        final List<Kra> kras = new ArrayList<>();
        final boolean[] failed = {false};
        final int[] remaining = {3};

        Runnable done = () -> {
            if (--remaining[0] == 0) {
                live.setValue(failed[0]
                        ? Resource.error("Couldn't load your performance data.")
                        : Resource.success(new PerformanceOverview(reviews, goals, kras)));
            }
        };

        client.get("performance_reviews?select=*&order=sort_order.asc", resp -> {
            if (resp.isSuccess()) {
                parseReviews(resp.body, reviews);
            } else {
                failed[0] = true;
            }
            done.run();
        });
        client.get("goals?select=*&order=sort_order.asc", resp -> {
            if (resp.isSuccess()) {
                parseGoals(resp.body, goals);
            } else {
                failed[0] = true;
            }
            done.run();
        });
        client.get("kras?select=*&order=sort_order.asc", resp -> {
            if (resp.isSuccess()) {
                parseKras(resp.body, kras);
            } else {
                failed[0] = true;
            }
            done.run();
        });
        return live;
    }

    // ===================== JSON mapping =====================

    private static void parseReviews(@NonNull String json, @NonNull List<PerformanceReview> out) {
        forEach(json, o -> {
            PerformanceReview.Status status = reviewStatus(o.optString("status", null));
            if (status != null) {
                String rating = o.isNull("rating_label") ? null : o.optString("rating_label", null);
                out.add(new PerformanceReview(
                        o.optString("id"), o.optString("cycle_name"), o.optString("period_label"),
                        o.optString("reviewer_name"), rating, status));
            }
        });
    }

    private static void parseGoals(@NonNull String json, @NonNull List<Goal> out) {
        forEach(json, o -> {
            Goal.Status status = goalStatus(o.optString("status", null));
            if (status != null) {
                out.add(new Goal(
                        o.optString("id"), o.optString("title"), o.optString("description"),
                        o.optInt("progress_percent"), o.optString("due_label"), status));
            }
        });
    }

    private static void parseKras(@NonNull String json, @NonNull List<Kra> out) {
        forEach(json, o -> out.add(new Kra(
                o.optString("id"), o.optString("title"), o.optInt("weightage_percent"),
                o.optString("target_label"), o.optString("achievement_label"), o.optString("rating_label"))));
    }

    @Nullable
    private static PerformanceReview.Status reviewStatus(@Nullable String s) {
        try {
            return s == null ? null : PerformanceReview.Status.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    private static Goal.Status goalStatus(@Nullable String s) {
        try {
            return s == null ? null : Goal.Status.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private interface RowConsumer {
        void accept(@NonNull JSONObject o);
    }

    private static void forEach(@NonNull String json, @NonNull RowConsumer consumer) {
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                consumer.accept(arr.getJSONObject(i));
            }
        } catch (Exception ignored) {
            // Return whatever parsed.
        }
    }
}
