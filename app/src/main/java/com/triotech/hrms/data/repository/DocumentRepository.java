package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Document;
import java.util.List;

/**
 * Data contract for the employee's documents. Backed by
 * {@link MockDocumentRepository} for now (a static demo catalog whose files are
 * generated on demand); a real document service can replace it later.
 */
public interface DocumentRepository {

    /** The full document catalog, grouped/ordered by category. */
    @NonNull
    LiveData<Resource<List<Document>>> observeDocuments();

    /** Looks up a single document by id (synchronous — the catalog is in memory). */
    @Nullable
    Document findById(@NonNull String id);
}
