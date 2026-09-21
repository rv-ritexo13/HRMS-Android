package com.triotech.hrms.core.base;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.triotech.hrms.core.util.Event;

/**
 * Common ViewModel base. Feature ViewModels (DashboardViewModel, EmployeesViewModel, ...)
 * extend this to get a ready-made one-shot error channel without re-plumbing LiveData
 * boilerplate on every screen.
 */
public abstract class BaseViewModel extends ViewModel {

    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();

    @NonNull
    public LiveData<Event<String>> getErrorEvent() {
        return errorEvent;
    }

    protected void postError(@NonNull String message) {
        errorEvent.postValue(new Event<>(message));
    }
}
