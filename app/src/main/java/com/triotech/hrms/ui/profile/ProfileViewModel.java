package com.triotech.hrms.ui.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.EmployeeProfile;
import com.triotech.hrms.data.repository.ProfileRepository;

/**
 * Backs {@link ProfileFragment} and {@link EditProfileFragment}: loads the
 * employee profile from the local database and (for the edit form) persists
 * changes back, re-reading so both screens reflect the saved state.
 */
public class ProfileViewModel extends BaseViewModel {

    private final ProfileRepository repository;
    private final String employeeId;
    private final MediatorLiveData<Resource<EmployeeProfile>> profile = new MediatorLiveData<>();
    @Nullable private LiveData<Resource<EmployeeProfile>> currentSource;

    public ProfileViewModel(@NonNull ProfileRepository repository, @NonNull String employeeId) {
        this.repository = repository;
        this.employeeId = employeeId;
        load();
    }

    @NonNull
    public LiveData<Resource<EmployeeProfile>> getProfile() {
        return profile;
    }

    public void retry() {
        load();
    }

    private void load() {
        if (currentSource != null) {
            profile.removeSource(currentSource);
        }
        currentSource = repository.observeProfile(employeeId);
        profile.addSource(currentSource, profile::setValue);
    }

    /** One-shot save stream for the edit form's loading/success/error state. */
    @NonNull
    public LiveData<Resource<EmployeeProfile>> save(@NonNull EmployeeProfile updated) {
        return repository.updateProfile(updated);
    }
}
