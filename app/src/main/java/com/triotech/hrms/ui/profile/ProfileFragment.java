package com.triotech.hrms.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.triotech.hrms.BuildConfig;
import com.triotech.hrms.R;
import com.triotech.hrms.core.base.BaseFragment;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.core.util.SessionManager;
import com.triotech.hrms.core.util.ThemePreferences;
import com.triotech.hrms.data.model.AuthUser;
import com.triotech.hrms.data.model.EmployeeProfile;
import com.triotech.hrms.data.repository.ProfileRepository;
import com.triotech.hrms.databinding.FragmentProfileBinding;
import com.triotech.hrms.databinding.ItemProfileFieldBinding;
import com.triotech.hrms.ui.auth.LoginActivity;
import com.triotech.hrms.ui.components.ConfirmDialogFragment;

/**
 * Professional employee profile: header (avatar/name/id/role + quick contact),
 * Personal / Employment / Emergency sections, an entry to the Documents screen,
 * plus the Phase 2 dark-mode toggle and Log out flow (unchanged). Profile data is
 * loaded from the local database via {@link ProfileViewModel}; editing opens
 * {@link EditProfileFragment}.
 */
public class ProfileFragment extends BaseFragment<FragmentProfileBinding>
        implements ConfirmDialogFragment.Listener {

    private static final String REQUEST_KEY_LOGOUT = "logout";

    private ProfileViewModel viewModel;

    @Override
    protected FragmentProfileBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        getBinding().textAppVersion.setText(
                getString(R.string.profile_app_version_format, BuildConfig.VERSION_NAME));

        getBinding().switchDarkMode.setChecked(ThemePreferences.isDarkModeActive(requireContext()));
        getBinding().switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int mode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            ThemePreferences.setNightMode(requireContext(), mode);
        });
        getBinding().buttonLogout.setOnClickListener(v -> showLogoutConfirmation());

        getBinding().buttonEditProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_profile_to_editProfile));
        getBinding().cardDocuments.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_profile_to_documents));

        AuthUser user = SessionManager.getSession(requireContext());
        String employeeId = user != null ? user.getEmployeeId() : "EMP001";

        ProfileRepository repository = ServiceLocator.getInstance().getProfileRepository();
        viewModel = new ViewModelProvider(this,
                new ViewModelFactory(() -> new ProfileViewModel(repository, employeeId)))
                .get(ProfileViewModel.class);

        getBinding().errorView.setTitle(R.string.profile_error_title);
        getBinding().errorView.setMessage(R.string.profile_error_message);
        getBinding().errorView.setOnRetryListener(viewModel::retry);
        getBinding().swipeRefresh.setOnRefreshListener(viewModel::retry);

        viewModel.getProfile().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-read after returning from Edit Profile so saved changes show immediately.
        if (viewModel != null) {
            viewModel.retry();
        }
    }

    private void render(@NonNull Resource<EmployeeProfile> resource) {
        boolean success = resource.isSuccess() && resource.data != null;
        getBinding().swipeRefresh.setRefreshing(resource.isLoading() && success);

        getBinding().loadingView.setVisibility(
                resource.isLoading() && !success ? View.VISIBLE : View.GONE);
        getBinding().errorView.setVisibility(resource.isError() ? View.VISIBLE : View.GONE);
        getBinding().swipeRefresh.setVisibility(success ? View.VISIBLE : View.GONE);

        if (success) {
            bind(resource.data);
        }
    }

    private void bind(@NonNull EmployeeProfile p) {
        getBinding().textHeaderInitials.setText(p.getInitials());
        getBinding().textHeaderName.setText(p.getFullName());
        getBinding().textHeaderRole.setText(p.getDesignation() + " · " + p.getDepartment());
        getBinding().textHeaderId.setText(p.getEmployeeId());

        field(getBinding().qEmail, R.drawable.ic_email, R.string.profile_quick_email, p.getWorkEmail());
        field(getBinding().qPhone, R.drawable.ic_phone, R.string.profile_quick_phone, p.getPhoneNumber());

        // Personal Information
        field(getBinding().pDob, R.drawable.ic_calendar, R.string.profile_field_dob,
                DateUtils.formatLongDate(p.getDateOfBirthMillis()));
        field(getBinding().pGender, R.drawable.ic_person, R.string.profile_field_gender, p.getGender());
        field(getBinding().pPhone, R.drawable.ic_phone, R.string.profile_field_phone, p.getPhoneNumber());
        field(getBinding().pEmail, R.drawable.ic_email, R.string.profile_field_email, p.getWorkEmail());

        // Employment Information
        field(getBinding().eId, R.drawable.ic_badge, R.string.profile_field_employee_id, p.getEmployeeId());
        field(getBinding().eDept, R.drawable.ic_work_outline, R.string.profile_field_department, p.getDepartment());
        field(getBinding().eDesignation, R.drawable.ic_work_outline, R.string.profile_field_designation,
                p.getDesignation());
        field(getBinding().eManager, R.drawable.ic_people, R.string.profile_field_manager, p.getReportingManager());
        field(getBinding().eJoining, R.drawable.ic_calendar, R.string.profile_field_joining,
                DateUtils.formatLongDate(p.getJoiningDateMillis()));
        field(getBinding().eType, R.drawable.ic_work_outline, R.string.profile_field_employment_type,
                p.getEmploymentType());
        field(getBinding().eLocation, R.drawable.ic_location, R.string.profile_field_location,
                p.getOfficeLocation());

        // Emergency Contact
        field(getBinding().emName, R.drawable.ic_person, R.string.profile_field_emergency_name,
                p.getEmergencyName());
        field(getBinding().emRelationship, R.drawable.ic_emergency, R.string.profile_field_emergency_relationship,
                p.getEmergencyRelationship());
        field(getBinding().emPhone, R.drawable.ic_phone, R.string.profile_field_emergency_phone,
                p.getEmergencyPhone());
    }

    private void field(
            @NonNull ItemProfileFieldBinding row, @DrawableRes int icon, int labelRes, @NonNull String value) {
        row.imageFieldIcon.setImageResource(icon);
        row.textFieldLabel.setText(labelRes);
        row.textFieldValue.setText(value);
    }

    private void showLogoutConfirmation() {
        ConfirmDialogFragment.show(
                getChildFragmentManager(),
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
            SessionManager.clearSession(requireContext());
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        }
    }
}
