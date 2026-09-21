package com.triotech.hrms.ui.leave;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.LeaveRequest;
import com.triotech.hrms.data.repository.LeaveRepository;

/**
 * Backs {@link LeaveDetailFragment}: loads a single leave request by id and, for
 * pending requests, cancels it.
 */
public class LeaveDetailViewModel extends BaseViewModel {

    private final LeaveRepository repository;
    private final String requestId;
    private final MediatorLiveData<Resource<LeaveRequest>> request = new MediatorLiveData<>();
    @Nullable private LiveData<Resource<LeaveRequest>> currentSource;

    public LeaveDetailViewModel(@NonNull LeaveRepository repository, @NonNull String requestId) {
        this.repository = repository;
        this.requestId = requestId;
        load();
    }

    @NonNull
    public LiveData<Resource<LeaveRequest>> getRequest() {
        return request;
    }

    public void retry() {
        load();
    }

    private void load() {
        if (currentSource != null) {
            request.removeSource(currentSource);
        }
        currentSource = repository.observeRequest(requestId);
        request.addSource(currentSource, request::setValue);
    }

    @NonNull
    public LiveData<Resource<Boolean>> cancel() {
        return repository.cancelRequest(requestId);
    }
}
