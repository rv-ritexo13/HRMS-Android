package com.triotech.hrms.ui.leave;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.LeaveBalance;
import com.triotech.hrms.data.model.LeaveRequest;
import com.triotech.hrms.data.repository.LeaveRepository;
import java.util.List;

/**
 * Backs {@link LeaveFragment}: exposes leave balances and the request history as
 * two {@code Resource} streams and reloads both on demand (pull-to-refresh, or
 * after returning from Apply Leave / a cancellation).
 */
public class LeaveViewModel extends BaseViewModel {

    private final LeaveRepository repository;

    private final MediatorLiveData<Resource<List<LeaveBalance>>> balances = new MediatorLiveData<>();
    private final MediatorLiveData<Resource<List<LeaveRequest>>> requests = new MediatorLiveData<>();
    @Nullable private LiveData<Resource<List<LeaveBalance>>> balancesSource;
    @Nullable private LiveData<Resource<List<LeaveRequest>>> requestsSource;

    public LeaveViewModel(@NonNull LeaveRepository repository) {
        this.repository = repository;
        refresh();
    }

    @NonNull
    public LiveData<Resource<List<LeaveBalance>>> getBalances() {
        return balances;
    }

    @NonNull
    public LiveData<Resource<List<LeaveRequest>>> getRequests() {
        return requests;
    }

    public void refresh() {
        if (balancesSource != null) {
            balances.removeSource(balancesSource);
        }
        balancesSource = repository.observeBalances();
        balances.addSource(balancesSource, balances::setValue);

        if (requestsSource != null) {
            requests.removeSource(requestsSource);
        }
        requestsSource = repository.observeRequests();
        requests.addSource(requestsSource, requests::setValue);
    }
}
