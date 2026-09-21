package com.triotech.hrms.ui.profile;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;
import com.triotech.hrms.R;
import com.triotech.hrms.core.base.BaseFragment;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.core.util.SessionManager;
import com.triotech.hrms.data.model.AuthUser;
import com.triotech.hrms.data.model.EmployeeProfile;
import com.triotech.hrms.data.repository.ProfileRepository;
import com.triotech.hrms.databinding.FragmentEditProfileBinding;

/**
 * Edit form for the user-editable subset of the profile (name, DOB, gender,
 * contact, location and emergency contact). HR-controlled fields are preserved.
 * Validates inline (required fields, email and phone format) before persisting via
 * {@link ProfileViewModel#save}; on success it returns to the profile screen, which
 * re-reads the saved values from the database.
 */
public class EditProfileFragment extends BaseFragment<FragmentEditProfileBinding> {

    private ProfileViewModel viewModel;
    @Nullable private EmployeeProfile loaded;
    private long dobMillis;

    @Override
    protected FragmentEditProfileBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentEditProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        getBinding().toolbar.setNavigationOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());

        getBinding().editGender.setSimpleItems(new String[] {"Male", "Female", "Other"});
        getBinding().editDob.setOnClickListener(v -> showDatePicker());
        getBinding().buttonSave.setOnClickListener(v -> onSave());

        AuthUser user = SessionManager.getSession(requireContext());
        String employeeId = user != null ? user.getEmployeeId() : "EMP001";
        ProfileRepository repository = ServiceLocator.getInstance().getProfileRepository();
        viewModel = new ViewModelProvider(this,
                new ViewModelFactory(() -> new ProfileViewModel(repository, employeeId)))
                .get(ProfileViewModel.class);

        getBinding().errorView.setTitle(R.string.profile_error_title);
        getBinding().errorView.setMessage(R.string.profile_error_message);
        getBinding().errorView.setOnRetryListener(viewModel::retry);

        viewModel.getProfile().observe(getViewLifecycleOwner(), this::renderLoad);
    }

    private void renderLoad(@NonNull Resource<EmployeeProfile> resource) {
        boolean success = resource.isSuccess() && resource.data != null;
        getBinding().loadingView.setVisibility(resource.isLoading() ? View.VISIBLE : View.GONE);
        getBinding().errorView.setVisibility(resource.isError() ? View.VISIBLE : View.GONE);
        getBinding().formContainer.setVisibility(success ? View.VISIBLE : View.GONE);
        if (success && loaded == null) {
            prefill(resource.data);
        }
    }

    private void prefill(@NonNull EmployeeProfile p) {
        loaded = p;
        dobMillis = p.getDateOfBirthMillis();
        getBinding().editName.setText(p.getFullName());
        getBinding().editDob.setText(DateUtils.formatLongDate(dobMillis));
        getBinding().editGender.setText(p.getGender(), false);
        getBinding().editPhone.setText(p.getPhoneNumber());
        getBinding().editEmail.setText(p.getWorkEmail());
        getBinding().editLocation.setText(p.getOfficeLocation());
        getBinding().editEmergencyName.setText(p.getEmergencyName());
        getBinding().editEmergencyRelationship.setText(p.getEmergencyRelationship());
        getBinding().editEmergencyPhone.setText(p.getEmergencyPhone());
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.edit_profile_hint_dob)
                .setSelection(dobMillis > 0 ? dobMillis : MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            dobMillis = selection;
            getBinding().editDob.setText(DateUtils.formatLongDate(dobMillis));
            tilFor(getBinding().editDob).setError(null);
        });
        picker.show(getChildFragmentManager(), "dob_picker");
    }

    private void onSave() {
        if (loaded == null) {
            return;
        }
        String name = text(getBinding().editName);
        String gender = text(getBinding().editGender);
        String phone = text(getBinding().editPhone);
        String email = text(getBinding().editEmail);
        String location = text(getBinding().editLocation);
        String emName = text(getBinding().editEmergencyName);
        String emRel = text(getBinding().editEmergencyRelationship);
        String emPhone = text(getBinding().editEmergencyPhone);

        boolean valid = true;
        valid &= require(getBinding().editName, name, R.string.edit_profile_err_name);
        if (dobMillis <= 0) {
            tilFor(getBinding().editDob).setError(getString(R.string.edit_profile_err_required));
            valid = false;
        } else {
            tilFor(getBinding().editDob).setError(null);
        }
        valid &= require(getBinding().editGender, gender, R.string.edit_profile_err_required);
        valid &= validPhone(getBinding().editPhone, phone);
        valid &= validEmail(getBinding().editEmail, email);
        valid &= require(getBinding().editLocation, location, R.string.edit_profile_err_required);
        valid &= require(getBinding().editEmergencyName, emName, R.string.edit_profile_err_required);
        valid &= require(getBinding().editEmergencyRelationship, emRel, R.string.edit_profile_err_required);
        valid &= validPhone(getBinding().editEmergencyPhone, emPhone);

        if (!valid) {
            return;
        }

        EmployeeProfile updated = loaded.withEdits(
                name, dobMillis, gender, phone, email, location, emName, emRel, emPhone);

        getBinding().buttonSave.setEnabled(false);
        viewModel.save(updated).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess()) {
                Toast.makeText(requireContext(), R.string.edit_profile_saved, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            } else if (resource.isError()) {
                getBinding().buttonSave.setEnabled(true);
                Toast.makeText(requireContext(), R.string.edit_profile_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean require(@NonNull EditText field, @NonNull String value, int errorRes) {
        if (value.isEmpty()) {
            tilFor(field).setError(getString(errorRes));
            return false;
        }
        tilFor(field).setError(null);
        return true;
    }

    private boolean validEmail(@NonNull EditText field, @NonNull String value) {
        if (value.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            tilFor(field).setError(getString(R.string.edit_profile_err_email));
            return false;
        }
        tilFor(field).setError(null);
        return true;
    }

    private boolean validPhone(@NonNull EditText field, @NonNull String value) {
        if (value.replaceAll("\\D", "").length() < 10) {
            tilFor(field).setError(getString(R.string.edit_profile_err_phone));
            return false;
        }
        tilFor(field).setError(null);
        return true;
    }

    @NonNull
    private static String text(@NonNull EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    @NonNull
    private static TextInputLayout tilFor(@NonNull EditText field) {
        // editText -> FrameLayout -> TextInputLayout
        return (TextInputLayout) field.getParent().getParent();
    }
}
