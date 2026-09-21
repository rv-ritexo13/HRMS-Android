package com.triotech.hrms.ui.leave;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.LeaveRequest;
import com.triotech.hrms.data.repository.LeaveRepository;

/**
 * Backs {@link ApplyLeaveFragment}: submits a new (pending) leave request to the
 * repository and exposes the one-shot loading/success/error stream.
 */
public class ApplyLeaveViewModel extends BaseViewModel {

    private final LeaveRepository repository;

    public ApplyLeaveViewModel(@NonNull LeaveRepository repository) {
        this.repository = repository;
    }

    @NonNull
    public LiveData<Resource<LeaveRequest>> apply(@NonNull LeaveRequest draft) {
        return repository.applyLeave(draft);
    }
}
