package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.network.SupabaseClient;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Announcement;
import com.triotech.hrms.data.model.DashboardSummary;
import com.triotech.hrms.data.model.Holiday;
import com.triotech.hrms.data.model.NotificationItem;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * {@link DashboardContentRepository} backed by Supabase. Reads announcements,
 * holidays and notifications from PostgREST (announcements/holidays are global,
 * notifications are RLS-scoped to the signed-in user). The mini-card summary was
 * removed from the Home screen, so {@link #getSummary()} is a no-op.
 */
public class SupabaseDashboardContentRepository implements DashboardContentRepository {

    private final SupabaseClient client = SupabaseClient.getInstance();

    @NonNull
    @Override
    public LiveData<Resource<DashboardSummary>> getSummary() {
        // The Home mini-cards were removed; nothing observes this any more.
        MutableLiveData<Resource<DashboardSummary>> live = new MutableLiveData<>();
        live.setValue(Resource.empty());
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<List<Announcement>>> getAnnouncements() {
        MutableLiveData<Resource<List<Announcement>>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get("announcements?select=*&order=sort_order.asc", resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load announcements."));
                return;
            }
            List<Announcement> list = new ArrayList<>();
            forEach(resp.body, o -> list.add(new Announcement(
                    o.optString("id"), o.optString("title"), o.optString("body"),
                    o.optString("time_label", ""))));
            live.setValue(list.isEmpty() ? Resource.empty() : Resource.success(list));
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<List<Holiday>>> getUpcomingHolidays() {
        MutableLiveData<Resource<List<Holiday>>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get("holidays?select=*&order=sort_order.asc", resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load holidays."));
                return;
            }
            List<Holiday> list = new ArrayList<>();
            forEach(resp.body, o -> list.add(new Holiday(
                    o.optString("id"), o.optString("day_number"), o.optString("month_abbrev"),
                    o.optString("name"), o.optString("day_of_week", ""))));
            live.setValue(list.isEmpty() ? Resource.empty() : Resource.success(list));
        });
        return live;
    }

    @NonNull
    @Override
    public LiveData<Resource<List<NotificationItem>>> getRecentNotifications() {
        MutableLiveData<Resource<List<NotificationItem>>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get("notifications?select=*&order=sort_order.asc", resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load notifications."));
                return;
            }
            List<NotificationItem> list = new ArrayList<>();
            forEach(resp.body, o -> list.add(new NotificationItem(
                    o.optString("id"), o.optString("title"), o.optString("body"),
                    o.optString("time_label", ""), o.optBoolean("is_read", false))));
            live.setValue(list.isEmpty() ? Resource.empty() : Resource.success(list));
        });
        return live;
    }

    // ===================== helpers =====================

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
