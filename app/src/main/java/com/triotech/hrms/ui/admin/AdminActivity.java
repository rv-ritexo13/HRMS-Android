package com.triotech.hrms.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.triotech.hrms.R;
import com.triotech.hrms.core.util.SessionManager;
import com.triotech.hrms.data.model.AuthUser;
import com.triotech.hrms.databinding.ActivityAdminDashboardBinding;
import com.triotech.hrms.ui.auth.LoginActivity;
import com.triotech.hrms.ui.components.ConfirmDialogFragment;

/**
 * Admin landing screen. Per Phase 2 scope, this is a clean placeholder — employee
 * management, approvals and org-wide reporting are out of scope until an
 * Admin-focused phase defines them. It exists so {@code UserRole.ADMIN} logins
 * (ADMIN001 / 123456) have somewhere real to land, with a working logout.
 */
public class AdminActivity extends AppCompatActivity implements ConfirmDialogFragment.Listener {

    private static final String REQUEST_KEY_LOGOUT = "admin_logout";

    private ActivityAdminDashboardBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AuthUser user = SessionManager.getSession(this);
        if (user != null) {
            binding.toolbarAdmin.setSubtitle(user.getFullName());
        }

        binding.emptyStateView.setIcon(R.drawable.ic_work_outline);
        binding.emptyStateView.setTitle(R.string.admin_dashboard_empty_title);
        binding.emptyStateView.setMessage(R.string.admin_dashboard_empty_message);

        binding.toolbarAdmin.inflateMenu(R.menu.admin_toolbar_menu);
        binding.toolbarAdmin.setOnMenuItemClickListener(this::onMenuItemClick);
    }

    private boolean onMenuItemClick(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.actionLogout) {
            showLogoutConfirmation();
            return true;
        }
        return false;
    }

    private void showLogoutConfirmation() {
        ConfirmDialogFragment.show(
                getSupportFragmentManager(),
                REQUEST_KEY_LOGOUT,
                R.string.profile_logout_dialog_title,
                R.string.profile_logout_dialog_message,
                R.drawable.ic_logout,
                R.string.action_confirm,
                R.string.action_cancel,
                this);
    }

    @Override
    public void onConfirmed(@NonNull String requestKey) {
        if (REQUEST_KEY_LOGOUT.equals(requestKey)) {
            SessionManager.clearSession(this);
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}
