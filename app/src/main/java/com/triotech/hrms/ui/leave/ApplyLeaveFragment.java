package com.triotech.hrms.ui.leave;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.triotech.hrms.R;
import com.triotech.hrms.core.base.BaseFragment;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.data.model.LeaveRequest;
import com.triotech.hrms.data.model.LeaveStatus;
import com.triotech.hrms.data.model.LeaveType;
import com.triotech.hrms.data.repository.LeaveRepository;
import com.triotech.hrms.databinding.FragmentApplyLeaveBinding;

/**
 * Apply Leave form. Validates leave type, a valid date range and a reason before
 * creating a PENDING {@link LeaveRequest} in the database. An optional attachment
 * can be picked (its display name is recorded). On success it returns to the Leave
 * screen, which reloads to show the new request.
 */
public class ApplyLeaveFragment extends BaseFragment<FragmentApplyLeaveBinding> {

    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private ApplyLeaveViewModel viewModel;
    @Nullable private LeaveType selectedType;
    private long startMillis;
    private long endMillis;
    @Nullable private String attachmentName;

    private final ActivityResultLauncher<String> pickAttachment =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onAttachmentPicked);

    @Override
    protected FragmentApplyLeaveBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentApplyLeaveBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        getBinding().toolbar.setNavigationOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());

        LeaveType[] types = LeaveType.values();
        String[] labels = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            labels[i] = getString(LeavePresenter.labelFor(types[i]));
        }
        getBinding().editType.setSimpleItems(labels);
        getBinding().editType.setOnItemClickListener((parent, view, position, id) -> {
            selectedType = types[position];
            getBinding().layoutType.setError(null);
        });

        getBinding().editStart.setOnClickListener(v -> pickDate(true));
        getBinding().editEnd.setOnClickListener(v -> pickDate(false));
        getBinding().buttonAttachment.setOnClickListener(v -> pickAttachment.launch("*/*"));
        getBinding().buttonSubmit.setOnClickListener(v -> submit());

        LeaveRepository repository = ServiceLocator.getInstance().getLeaveRepository();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(() -> new ApplyLeaveViewModel(repository)))
                .get(ApplyLeaveViewModel.class);
    }

    private void pickDate(boolean isStart) {
        long current = isStart ? startMillis : endMillis;
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStart ? R.string.apply_leave_hint_start : R.string.apply_leave_hint_end)
                .setSelection(current > 0 ? current : MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (isStart) {
                startMillis = selection;
                getBinding().editStart.setText(DateUtils.formatLongDate(selection));
                getBinding().layoutStart.setError(null);
                // Keep end >= start.
                if (endMillis > 0 && endMillis < startMillis) {
                    endMillis = startMillis;
                    getBinding().editEnd.setText(DateUtils.formatLongDate(endMillis));
                }
            } else {
                endMillis = selection;
                getBinding().editEnd.setText(DateUtils.formatLongDate(selection));
                getBinding().layoutEnd.setError(null);
            }
            updateDays();
        });
        picker.show(getChildFragmentManager(), isStart ? "start_picker" : "end_picker");
    }

    private void updateDays() {
        if (startMillis > 0 && endMillis >= startMillis) {
            int days = (int) ((endMillis - startMillis) / DAY_MS) + 1;
            getBinding().editDays.setText(getString(R.string.leave_days_format, days));
        } else {
            getBinding().editDays.setText("");
        }
    }

    private void onAttachmentPicked(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        attachmentName = resolveDisplayName(uri);
        getBinding().textAttachmentName.setText(
                attachmentName != null ? attachmentName : getString(R.string.apply_leave_attachment_none));
    }

    @Nullable
    private String resolveDisplayName(@NonNull Uri uri) {
        try (Cursor cursor = requireContext().getContentResolver()
                .query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return uri.getLastPathSegment();
    }

    private void submit() {
        boolean valid = true;
        if (selectedType == null) {
            getBinding().layoutType.setError(getString(R.string.apply_leave_err_type));
            valid = false;
        }
        if (startMillis <= 0) {
            getBinding().layoutStart.setError(getString(R.string.apply_leave_err_start));
            valid = false;
        }
        if (endMillis <= 0) {
            getBinding().layoutEnd.setError(getString(R.string.apply_leave_err_end));
            valid = false;
        }
        if (startMillis > 0 && endMillis > 0 && endMillis < startMillis) {
            getBinding().layoutEnd.setError(getString(R.string.apply_leave_err_range));
            valid = false;
        }
        String reason = getBinding().editReason.getText() == null
                ? "" : getBinding().editReason.getText().toString().trim();
        if (reason.isEmpty()) {
            getBinding().layoutReason.setError(getString(R.string.apply_leave_err_reason));
            valid = false;
        } else {
            getBinding().layoutReason.setError(null);
        }

        if (!valid || selectedType == null) {
            return;
        }

        int days = (int) ((endMillis - startMillis) / DAY_MS) + 1;
        LeaveRequest draft = new LeaveRequest(
                "LR-" + System.currentTimeMillis(),
                selectedType,
                startMillis,
                endMillis,
                days,
                reason,
                LeaveStatus.PENDING,
                System.currentTimeMillis(),
                "",
                attachmentName);

        getBinding().buttonSubmit.setEnabled(false);
        getBinding().loadingView.setVisibility(View.VISIBLE);
        viewModel.apply(draft).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess()) {
                Toast.makeText(requireContext(), R.string.apply_leave_submitted, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            } else if (resource.isError()) {
                getBinding().loadingView.setVisibility(View.GONE);
                getBinding().buttonSubmit.setEnabled(true);
                Toast.makeText(requireContext(), R.string.apply_leave_error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
