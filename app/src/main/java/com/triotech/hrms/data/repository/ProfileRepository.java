package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.EmployeeProfile;

/**
 * Data contract for the employee profile. Backed by {@link DbProfileRepository}
 * reading/writing the seeded {@code profile} table in the local database, so edits
 * persist across restarts. A real HR API can replace it behind
 * {@link com.triotech.hrms.core.di.ServiceLocator} with no UI change.
 */
public interface ProfileRepository {

    /** The profile for the given employee id, or an error if none exists. */
    @NonNull
    LiveData<Resource<EmployeeProfile>> observeProfile(@NonNull String employeeId);

    /** Persists edits to the profile. Emits loading, then success with the saved profile, or error. */
    @NonNull
    LiveData<Resource<EmployeeProfile>> updateProfile(@NonNull EmployeeProfile profile);
}
