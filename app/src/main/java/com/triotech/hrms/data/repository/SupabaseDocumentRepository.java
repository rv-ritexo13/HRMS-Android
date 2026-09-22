package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.network.SupabaseClient;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Document;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * {@link DocumentRepository} backed by Supabase PostgREST. Reads the document
 * catalog (metadata) from the {@code documents} table under the signed-in token
 * (RLS-scoped). The files themselves are still materialised on demand by
 * {@code SampleDocumentGenerator} — only the catalog moved to the backend.
 *
 * <p>The last loaded catalog is cached so {@link #findById} (called synchronously
 * by the viewer, after the list has loaded) can resolve without another call.</p>
 */
public class SupabaseDocumentRepository implements DocumentRepository {

    private static final String TABLE = "documents";

    private final SupabaseClient client = SupabaseClient.getInstance();
    private volatile List<Document> cache = Collections.emptyList();

    @NonNull
    @Override
    public LiveData<Resource<List<Document>>> observeDocuments() {
        MutableLiveData<Resource<List<Document>>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        client.get(TABLE + "?select=*&order=sort_order.asc", resp -> {
            if (!resp.isSuccess()) {
                live.setValue(Resource.error("Couldn't load your documents."));
                return;
            }
            List<Document> list = parseList(resp.body);
            cache = list;
            live.setValue(list.isEmpty() ? Resource.empty() : Resource.success(list));
        });
        return live;
    }

    @Nullable
    @Override
    public Document findById(@NonNull String id) {
        for (Document d : cache) {
            if (d.getId().equals(id)) {
                return d;
            }
        }
        return null;
    }

    // ===================== JSON mapping =====================

    @NonNull
    private static List<Document> parseList(@NonNull String json) {
        List<Document> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                Document d = fromJson(arr.getJSONObject(i));
                if (d != null) {
                    out.add(d);
                }
            }
        } catch (Exception ignored) {
            // Return whatever parsed.
        }
        return out;
    }

    @Nullable
    private static Document fromJson(@NonNull JSONObject o) {
        Document.Category category = category(o.optString("category", null));
        Document.FileType type = fileType(o.optString("file_type", null));
        if (category == null || type == null) {
            return null;
        }
        return new Document(
                o.optString("id"), category, o.optString("name"), type,
                o.optLong("date_millis"), o.optLong("size_bytes"));
    }

    @Nullable
    private static Document.Category category(@Nullable String s) {
        try {
            return s == null ? null : Document.Category.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Document.Category.OTHER;
        }
    }

    @Nullable
    private static Document.FileType fileType(@Nullable String s) {
        try {
            return s == null ? null : Document.FileType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Document.FileType.PDF;
        }
    }
}
