package com.triotech.hrms.ui.documents;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Document;
import com.triotech.hrms.data.repository.DocumentRepository;
import java.util.List;

/**
 * Backs {@link DocumentsFragment}. Exposes the document catalog as one
 * {@code Resource} stream so the screen drives Loading/Empty/Error/Success from a
 * single observer, with pull-to-refresh support.
 */
public class DocumentsViewModel extends BaseViewModel {

    private final DocumentRepository repository;
    private final MediatorLiveData<Resource<List<Document>>> documents = new MediatorLiveData<>();
    @Nullable private LiveData<Resource<List<Document>>> currentSource;

    public DocumentsViewModel(@NonNull DocumentRepository repository) {
        this.repository = repository;
        load();
    }

    @NonNull
    public LiveData<Resource<List<Document>>> getDocuments() {
        return documents;
    }

    public void retry() {
        load();
    }

    private void load() {
        if (currentSource != null) {
            documents.removeSource(currentSource);
        }
        currentSource = repository.observeDocuments();
        documents.addSource(currentSource, documents::setValue);
    }
}
