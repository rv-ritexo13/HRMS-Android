package com.triotech.hrms.ui.leave;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.triotech.hrms.R;
import com.triotech.hrms.core.base.BaseFragment;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.LeaveBalance;
import com.triotech.hrms.data.model.LeaveRequest;
import com.triotech.hrms.data.repository.LeaveRepository;
import com.triotech.hrms.databinding.FragmentLeaveBinding;
import java.util.Collections;
import java.util.List;

/**
 * Leave dashboard: horizontally scrolling balance cards, an Apply Leave action, and
 * the leave request history. Balances drive the shared Loading/Error states; the
 * history list shows an inline empty message when there are no requests. Reloads on
 * resume so a newly applied or cancelled request is reflected immediately.
 */
public class LeaveFragment extends BaseFragment<FragmentLeaveBinding> {

    private LeaveViewModel viewModel;
    private final LeaveBalanceAdapter balanceAdapter = new LeaveBalanceAdapter();
    private final LeaveRequestAdapter requestAdapter = new LeaveRequestAdapter();

    @Override
    protected FragmentLeaveBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentLeaveBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        getBinding().recyclerBalances.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        getBinding().recyclerBalances.setAdapter(balanceAdapter);

        getBinding().recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().recyclerHistory.setAdapter(requestAdapter);
        requestAdapter.setOnRequestClickListener(this::openDetail);

        getBinding().buttonApplyLeave.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_leave_to_applyLeave));

        LeaveRepository repository = ServiceLocator.getInstance().getLeaveRepository();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(() -> new LeaveViewModel(repository)))
                .get(LeaveViewModel.class);

        getBinding().errorView.setTitle(R.string.leave_error_title);
        getBinding().errorView.setMessage(R.string.leave_error_message);
        getBinding().errorView.setOnRetryListener(viewModel::refresh);
        getBinding().swipeRefresh.setOnRefreshListener(viewModel::refresh);

        viewModel.getBalances().observe(getViewLifecycleOwner(), this::renderBalances);
        viewModel.getRequests().observe(getViewLifecycleOwner(), this::renderRequests);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    private void renderBalances(@NonNull Resource<List<LeaveBalance>> resource) {
        boolean hasData = !balanceAdapter.getCurrentList().isEmpty();
        getBinding().swipeRefresh.setRefreshing(resource.isLoading() && hasData);
        getBinding().loadingView.setVisibility(resource.isLoading() && !hasData ? View.VISIBLE : View.GONE);
        getBinding().errorView.setVisibility(resource.isError() ? View.VISIBLE : View.GONE);
        getBinding().swipeRefresh.setVisibility(resource.isError() ? View.GONE : View.VISIBLE);

        if (resource.isSuccess() && resource.data != null) {
            balanceAdapter.submitList(resource.data);
        }
    }

    private void renderRequests(@NonNull Resource<List<LeaveRequest>> resource) {
        if (resource.isSuccess() && resource.data != null) {
            requestAdapter.submitList(resource.data);
            getBinding().textHistoryEmpty.setVisibility(resource.data.isEmpty() ? View.VISIBLE : View.GONE);
        } else if (resource.isEmpty()) {
            requestAdapter.submitList(Collections.emptyList());
            getBinding().textHistoryEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void openDetail(@NonNull LeaveRequest request) {
        Bundle args = new Bundle();
        args.putString(LeaveDetailFragment.ARG_REQUEST_ID, request.getId());
        NavHostFragment.findNavController(this).navigate(R.id.action_leave_to_leaveDetail, args);
    }
}
