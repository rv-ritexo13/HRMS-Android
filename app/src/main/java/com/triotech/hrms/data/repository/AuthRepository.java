package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.AuthUser;

/**
 * Authentication contract. {@link DbAuthRepository} backs this with the local
 * {@code users} table (seeded demo accounts plus any signed-up accounts); a real
 * implementation (network call, token storage) can replace it behind
 * {@link com.triotech.hrms.core.di.ServiceLocator} without touching
 * {@link com.triotech.hrms.ui.auth.LoginViewModel}.
 */
public interface AuthRepository {

    /**
     * Attempts to sign in with the given Employee ID (or email) and password.
     * Emits {@link Resource#loading()} immediately, then either
     * {@link Resource#success} with the matched user or {@link Resource#error}
     * with a user-facing message.
     */
    @NonNull
    LiveData<Resource<AuthUser>> login(@NonNull String employeeIdOrEmail, @NonNull String password);

    /**
     * Registers a new Employee account. Emits {@link Resource#loading()}, then
     * {@link Resource#success} with the created user, or {@link Resource#error}
     * (e.g. the Employee ID is already taken). A matching profile row is created
     * so the new user's Profile screen is populated.
     */
    @NonNull
    LiveData<Resource<AuthUser>> register(
            @NonNull String employeeId,
            @NonNull String fullName,
            @NonNull String email,
            @NonNull String password);
}
