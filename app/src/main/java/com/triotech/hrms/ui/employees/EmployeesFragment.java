package com.triotech.hrms.ui.employees;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.triotech.hrms.R;
import com.triotech.hrms.core.base.BaseFragment;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Employee;
import com.triotech.hrms.data.repository.EmployeeRepository;
import com.triotech.hrms.databinding.FragmentEmployeesBinding;
import java.util.List;

/**
 * Employee directory. Demonstrates the full state-component set (Loading /
 * Error / Empty / Success) driven off a single {@code Resource} stream, plus
 * pull-to-refresh, against the mock {@link EmployeeRepository}.
 */
public class EmployeesFragment extends BaseFragment<FragmentEmployeesBinding> {

    private EmployeesViewModel viewModel;
    private final EmployeeAdapter adapter = new EmployeeAdapter();

    @Override
    protected FragmentEmployeesBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentEmployeesBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        EmployeeRepository repository = ServiceLocator.getInstance().getEmployeeRepository();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(() -> new EmployeesViewModel(repository)))
                .get(EmployeesViewModel.class);

        getBinding().recyclerEmployees.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().recyclerEmployees.setAdapter(adapter);

        getBinding().swipeRefresh.setOnRefreshListener(() -> viewModel.retry());
        getBinding().errorView.setOnRetryListener(viewModel::retry);

        viewModel.getEmployees().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(@NonNull Resource<List<Employee>> resource) {
        getBinding().swipeRefresh.setRefreshing(resource.isLoading() && !adapter.getCurrentList().isEmpty());

        boolean showFullScreenLoading = resource.isLoading() && adapter.getCurrentList().isEmpty();
        getBinding().loadingView.setVisibility(showFullScreenLoading ? View.VISIBLE : View.GONE);
        getBinding().errorView.setVisibility(resource.isError() ? View.VISIBLE : View.GONE);
        getBinding().emptyStateView.setVisibility(resource.isEmpty() ? View.VISIBLE : View.GONE);
        getBinding().swipeRefresh.setVisibility(resource.isSuccess() ? View.VISIBLE : View.GONE);

        if (resource.isError() && resource.message != null) {
            getBinding().errorView.setMessage(R.string.employees_error_message);
        }

        if (resource.isSuccess() && resource.data != null) {
            adapter.submitList(resource.data);
            getBinding().textEmployeeCount.setText(
                    getString(R.string.employees_count_format, resource.data.size()));
        } else if (resource.isEmpty()) {
            getBinding().textEmployeeCount.setText(
                    getString(R.string.employees_count_format, 0));
        }
    }
}
