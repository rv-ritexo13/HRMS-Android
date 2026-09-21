package com.triotech.hrms.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.triotech.hrms.R;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.core.util.SessionManager;
import com.triotech.hrms.data.model.AuthUser;
import com.triotech.hrms.data.model.UserRole;
import com.triotech.hrms.data.repository.AuthRepository;
import com.triotech.hrms.databinding.ActivityLoginBinding;
import com.triotech.hrms.ui.admin.AdminActivity;
import com.triotech.hrms.ui.main.MainActivity;

/**
 * Launcher screen. Authentication is validated against the local {@code users}
 * table (see {@link com.triotech.hrms.data.repository.DbAuthRepository}) — seeded
 * demo accounts plus any signed-up accounts — routed by role to
 * {@link MainActivity} (Employee) or {@link AdminActivity} (Admin). New accounts
 * are created via {@link SignupActivity}.
 *
 * <p>A logged-in session (see {@link SessionManager}) is checked before any UI is
 * shown, so a returning user with "Remember me" on skips straight to their
 * dashboard instead of seeing a login-screen flash.</p>
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthUser existingSession = SessionManager.getSession(this);
        if (existingSession != null) {
            routeToDashboard(existingSession.getRole());
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AuthRepository authRepository = ServiceLocator.getInstance().getAuthRepository();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(() -> new LoginViewModel(authRepository)))
                .get(LoginViewModel.class);

        prefillRememberedEmployeeId();

        binding.buttonLogin.setOnClickListener(v -> attemptLogin());
        binding.textForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        binding.buttonSignup.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));

        viewModel.getLoginResult().observe(this, this::renderLoginResult);
    }

    private void prefillRememberedEmployeeId() {
        String rememberedId = SessionManager.getRememberedEmployeeId(this);
        if (rememberedId != null) {
            binding.editEmployeeId.setText(rememberedId);
            binding.checkboxRememberMe.setChecked(true);
        }
    }

    private void attemptLogin() {
        String employeeId = textOf(binding.editEmployeeId);
        String password = textOf(binding.editPassword);

        binding.inputLayoutEmployeeId.setError(null);
        binding.inputLayoutPassword.setError(null);
        binding.textLoginError.setVisibility(View.GONE);

        boolean valid = true;
        if (TextUtils.isEmpty(employeeId)) {
            binding.inputLayoutEmployeeId.setError(getString(R.string.login_error_field_required));
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.inputLayoutPassword.setError(getString(R.string.login_error_field_required));
            valid = false;
        }
        if (!valid) {
            return;
        }

        viewModel.login(this, employeeId, password, binding.checkboxRememberMe.isChecked());
    }

    private void renderLoginResult(@NonNull Resource<AuthUser> resource) {
        boolean loading = resource.isLoading();
        setFormEnabled(!loading);
        binding.progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);

        if (resource.isError()) {
            binding.textLoginError.setText(
                    resource.message != null ? resource.message : getString(R.string.login_error_invalid_credentials));
            binding.textLoginError.setVisibility(View.VISIBLE);
        }

        if (resource.isSuccess() && resource.data != null) {
            routeToDashboard(resource.data.getRole());
        }
    }

    private void setFormEnabled(boolean enabled) {
        binding.editEmployeeId.setEnabled(enabled);
        binding.editPassword.setEnabled(enabled);
        binding.checkboxRememberMe.setEnabled(enabled);
        binding.buttonLogin.setEnabled(enabled);
        binding.textForgotPassword.setEnabled(enabled);
    }

    private void showForgotPasswordDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.login_forgot_password_title)
                .setMessage(R.string.login_forgot_password_message)
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    private void routeToDashboard(@NonNull UserRole role) {
        Class<?> destination = role == UserRole.ADMIN ? AdminActivity.class : MainActivity.class;
        startActivity(new Intent(this, destination));
        finish();
    }

    @NonNull
    private static String textOf(@NonNull TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
