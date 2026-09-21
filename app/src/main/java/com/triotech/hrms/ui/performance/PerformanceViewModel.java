package com.triotech.hrms.ui.performance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.PerformanceOverview;
import com.triotech.hrms.data.repository.PerformanceRepository;

/**
 * Backs {@link PerformanceFragment}. Exposes the performance overview as one
 * {@code Resource} stream so the screen drives Loading/Error/Success from a
 * single observer, with pull-to-refresh support.
 */
public class PerformanceViewModel extends BaseViewModel {

    private final PerformanceRepository repository;
    private final MediatorLiveData<Resource<PerformanceOverview>> performance = new MediatorLiveData<>();
    @Nullable private LiveData<Resource<PerformanceOverview>> currentSource;

    public PerformanceViewModel(@NonNull PerformanceRepository repository) {
        this.repository = repository;
        load();
    }

    @NonNull
    public LiveData<Resource<PerformanceOverview>> getPerformance() {
        return performance;
    }

    public void retry() {
        load();
    }

    private void load() {
        if (currentSource != null) {
            performance.removeSource(currentSource);
        }
        currentSource = repository.observePerformance();
        performance.addSource(currentSource, performance::setValue);
    }
}
