package com.triotech.hrms.ui.auth;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.core.util.SessionManager;
import com.triotech.hrms.data.model.AuthUser;
import com.triotech.hrms.data.repository.AuthRepository;

/**
 * Backs {@link SignupActivity}. Field validation lives in the Activity (driven off
 * TextInputLayout errors); this ViewModel owns the account-creation call and, on
 * success, signs the new user in by persisting the session.
 */
public class SignupViewModel extends BaseViewModel {

    private final AuthRepository authRepository;
    private final MediatorLiveData<Resource<AuthUser>> result = new MediatorLiveData<>();
    private LiveData<Resource<AuthUser>> currentSource;

    public SignupViewModel(@NonNull AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @NonNull
    public LiveData<Resource<AuthUser>> getResult() {
        return result;
    }

    public void register(
            @NonNull Context context, @NonNull String employeeId, @NonNull String fullName,
            @NonNull String email, @NonNull String password) {
        if (currentSource != null) {
            result.removeSource(currentSource);
        }
        currentSource = authRepository.register(employeeId, fullName, email, password);
        result.addSource(currentSource, resource -> {
            if (resource.isSuccess() && resource.data != null) {
                // Auto sign-in: persist session (not "remembered" across cold starts).
                SessionManager.saveSession(context.getApplicationContext(), resource.data, false);
            }
            result.setValue(resource);
        });
    }
}
