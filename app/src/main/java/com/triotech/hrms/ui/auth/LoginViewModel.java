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
 * Backs {@link LoginActivity}. Field-level "required" validation is handled in
 * the Activity (it's pure UI concern, driven off TextInputLayout's own error
 * display); this ViewModel owns the actual sign-in call and, on success,
 * persisting the session via {@link SessionManager}.
 */
public class LoginViewModel extends BaseViewModel {

    private final AuthRepository authRepository;
    private final MediatorLiveData<Resource<AuthUser>> loginResult = new MediatorLiveData<>();
    private LiveData<Resource<AuthUser>> currentSource;

    public LoginViewModel(@NonNull AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @NonNull
    public LiveData<Resource<AuthUser>> getLoginResult() {
        return loginResult;
    }

    public void login(@NonNull Context context, @NonNull String employeeId, @NonNull String password, boolean rememberMe) {
        if (currentSource != null) {
            loginResult.removeSource(currentSource);
        }
        currentSource = authRepository.login(employeeId, password);
        loginResult.addSource(currentSource, resource -> {
            if (resource.isSuccess() && resource.data != null) {
                SessionManager.saveSession(context.getApplicationContext(), resource.data, rememberMe);
            }
            loginResult.setValue(resource);
        });
    }
}
