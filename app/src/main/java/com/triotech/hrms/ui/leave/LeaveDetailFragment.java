package com.triotech.hrms.ui.leave;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.triotech.hrms.R;
import com.triotech.hrms.core.base.BaseFragment;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.LeaveRequest;
import com.triotech.hrms.data.model.LeaveStatus;
import com.triotech.hrms.data.repository.LeaveRepository;
import com.triotech.hrms.databinding.FragmentLeaveDetailBinding;
import com.triotech.hrms.databinding.ItemProfileFieldBinding;
import com.triotech.hrms.ui.components.ConfirmDialogFragment;

/**
 * Full detail for a single leave request. Shows type, dates, days, reason, applied
 * date, status and manager comments. Pending requests can be cancelled (with a
 * confirmation), which updates the database and returns to the Leave screen.
 */
public class LeaveDetailFragment extends BaseFragment<FragmentLeaveDetailBinding>
        implements ConfirmDialogFragment.Listener {

    public static final String ARG_REQUEST_ID = "leaveRequestId";
    private static final String REQUEST_KEY_CANCEL = "cancel_leave";

    private LeaveDetailViewModel viewModel;

    @Override
    protected FragmentLeaveDetailBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentLeaveDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        String requestId = getArguments() != null ? getArguments().getString(ARG_REQUEST_ID) : null;
        if (requestId == null) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        getBinding().toolbar.setNavigationOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());

        getBinding().errorView.setTitle(R.string.leave_error_title);
        getBinding().errorView.setMessage(R.string.leave_error_message);
        getBinding().errorView.setOnRetryListener(() -> viewModel.retry());

        getBinding().buttonCancelRequest.setOnClickListener(v -> confirmCancel());

        LeaveRepository repository = ServiceLocator.getInstance().getLeaveRepository();
        viewModel = new ViewModelProvider(this,
                new ViewModelFactory(() -> new LeaveDetailViewModel(repository, requestId)))
                .get(LeaveDetailViewModel.class);

        viewModel.getRequest().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(@NonNull Resource<LeaveRequest> resource) {
        getBinding().loadingView.setVisibility(resource.isLoading() ? View.VISIBLE : View.GONE);
        getBinding().errorView.setVisibility(resource.isError() ? View.VISIBLE : View.GONE);
        getBinding().contentContainer.setVisibility(resource.isSuccess() ? View.VISIBLE : View.GONE);
        if (resource.isSuccess() && resource.data != null) {
            bind(resource.data);
        }
    }

    private void bind(@NonNull LeaveRequest r) {
        getBinding().imageTypeIcon.setImageResource(LeavePresenter.iconFor(r.getType()));
        getBinding().imageTypeIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), LeavePresenter.accentColorFor(r.getType()))));
        getBinding().textType.setText(LeavePresenter.labelFor(r.getType()));

        getBinding().chipStatus.setText(LeavePresenter.labelFor(r.getStatus()));
        getBinding().chipStatus.setTextColor(
                ContextCompat.getColor(requireContext(), LeavePresenter.statusTextColor(r.getStatus())));
        getBinding().chipStatus.setChipBackgroundColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), LeavePresenter.statusContainerColor(r.getStatus()))));

        field(getBinding().dStart, R.drawable.ic_calendar, R.string.leave_detail_start,
                DateUtils.formatLongDate(r.getStartMillis()));
        field(getBinding().dEnd, R.drawable.ic_calendar, R.string.leave_detail_end,
                DateUtils.formatLongDate(r.getEndMillis()));
        field(getBinding().dDays, R.drawable.ic_event_available, R.string.leave_detail_days,
                getString(R.string.leave_days_format, r.getDays()));
        field(getBinding().dApplied, R.drawable.ic_calendar, R.string.leave_detail_applied,
                DateUtils.formatLongDate(r.getAppliedMillis()));
        field(getBinding().dReason, R.drawable.ic_description, R.string.leave_detail_reason, r.getReason());

        if (r.getAttachmentName() != null && !r.getAttachmentName().isEmpty()) {
            getBinding().dAttachment.getRoot().setVisibility(View.VISIBLE);
            field(getBinding().dAttachment, R.drawable.ic_attach_file, R.string.leave_detail_attachment,
                    r.getAttachmentName());
        } else {
            getBinding().dAttachment.getRoot().setVisibility(View.GONE);
        }

        getBinding().textManagerComments.setText(
                r.hasManagerComments() ? r.getManagerComments() : getString(R.string.leave_detail_no_comments));

        getBinding().buttonCancelRequest.setVisibility(
                r.getStatus() == LeaveStatus.PENDING ? View.VISIBLE : View.GONE);
    }

    private void field(
            @NonNull ItemProfileFieldBinding row, @DrawableRes int icon, int labelRes, @NonNull String value) {
        row.imageFieldIcon.setImageResource(icon);
        row.textFieldLabel.setText(labelRes);
        row.textFieldValue.setText(value);
    }

    private void confirmCancel() {
        ConfirmDialogFragment.show(
                getChildFragmentManager(),
                REQUEST_KEY_CANCEL,
                R.string.leave_detail_cancel_dialog_title,
                R.string.leave_detail_cancel_dialog_message,
                R.drawable.ic_event_busy,
                R.string.leave_detail_cancel,
                R.string.action_cancel,
                this);
    }

    @Override
    public void onConfirmed(@NonNull String requestKey) {
        if (!REQUEST_KEY_CANCEL.equals(requestKey)) {
            return;
        }
        getBinding().loadingView.setVisibility(View.VISIBLE);
        viewModel.cancel().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess()) {
                Toast.makeText(requireContext(), R.string.leave_detail_cancelled, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            } else if (resource.isError()) {
                getBinding().loadingView.setVisibility(View.GONE);
                Toast.makeText(requireContext(), R.string.leave_detail_cancel_error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
