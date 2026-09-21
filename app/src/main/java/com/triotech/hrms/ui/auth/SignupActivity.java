package com.triotech.hrms.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputEditText;
import com.triotech.hrms.R;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.AuthUser;
import com.triotech.hrms.data.repository.AuthRepository;
import com.triotech.hrms.databinding.ActivitySignupBinding;
import com.triotech.hrms.ui.main.MainActivity;

/**
 * Sign-up screen. Creates a new Employee account in the local {@code users} table
 * via {@link SignupViewModel}, then auto-signs-in and routes to {@link MainActivity}.
 * All new accounts are Employees; admin accounts are provisioned separately.
 */
public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private SignupViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AuthRepository authRepository = ServiceLocator.getInstance().getAuthRepository();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(() -> new SignupViewModel(authRepository)))
                .get(SignupViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.buttonBackToLogin.setOnClickListener(v -> finish());
        binding.buttonSignup.setOnClickListener(v -> attemptSignup());

        viewModel.getResult().observe(this, this::renderResult);
    }

    private void attemptSignup() {
        String name = textOf(binding.editName);
        String employeeId = textOf(binding.editEmployeeId);
        String email = textOf(binding.editEmail);
        String password = textOf(binding.editPassword);
        String confirm = textOf(binding.editConfirmPassword);

        binding.layoutName.setError(null);
        binding.layoutEmployeeId.setError(null);
        binding.layoutEmail.setError(null);
        binding.layoutPassword.setError(null);
        binding.layoutConfirmPassword.setError(null);
        binding.textSignupError.setVisibility(View.GONE);

        boolean valid = true;
        if (TextUtils.isEmpty(name)) {
            binding.layoutName.setError(getString(R.string.signup_err_name));
            valid = false;
        }
        if (TextUtils.isEmpty(employeeId)) {
            binding.layoutEmployeeId.setError(getString(R.string.signup_err_employee_id));
            valid = false;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.setError(getString(R.string.signup_err_email));
            valid = false;
        }
        if (password.length() < 6) {
            binding.layoutPassword.setError(getString(R.string.signup_err_password));
            valid = false;
        }
        if (!password.equals(confirm)) {
            binding.layoutConfirmPassword.setError(getString(R.string.signup_err_confirm));
            valid = false;
        }
        if (!valid) {
            return;
        }

        viewModel.register(this, employeeId, name, email, password);
    }

    private void renderResult(@NonNull Resource<AuthUser> resource) {
        boolean loading = resource.isLoading();
        setFormEnabled(!loading);
        binding.progressSignup.setVisibility(loading ? View.VISIBLE : View.GONE);

        if (resource.isError()) {
            binding.textSignupError.setText(resource.message);
            binding.textSignupError.setVisibility(View.VISIBLE);
        }

        if (resource.isSuccess() && resource.data != null) {
            Toast.makeText(this, R.string.signup_success, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void setFormEnabled(boolean enabled) {
        binding.editName.setEnabled(enabled);
        binding.editEmployeeId.setEnabled(enabled);
        binding.editEmail.setEnabled(enabled);
        binding.editPassword.setEnabled(enabled);
        binding.editConfirmPassword.setEnabled(enabled);
        binding.buttonSignup.setEnabled(enabled);
    }

    @NonNull
    private static String textOf(@NonNull TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
