package com.triotech.hrms.ui.expenses;

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
import com.triotech.hrms.data.model.ExpenseReport;
import com.triotech.hrms.data.repository.ExpenseRepository;
import com.triotech.hrms.databinding.FragmentExpenseReportsBinding;
import java.util.Collections;
import java.util.List;

/**
 * Monthly expense reports: the employee's expenses grouped into one report per
 * calendar month. Selecting a month opens {@link ExpenseReportDetailFragment}.
 * Reloads on resume so a newly added expense shows up in its month's report.
 */
public class ExpenseReportsFragment extends BaseFragment<FragmentExpenseReportsBinding> {

    static final String ARG_MONTH_KEY = "monthKey";

    private ExpenseReportsViewModel viewModel;
    private final ExpenseReportAdapter adapter = new ExpenseReportAdapter();

    @Override
    protected FragmentExpenseReportsBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentExpenseReportsBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        getBinding().toolbar.setNavigationOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());

        getBinding().recyclerReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().recyclerReports.setAdapter(adapter);
        adapter.setOnReportClickListener(this::openReport);

        ExpenseRepository repository = ServiceLocator.getInstance().getExpenseRepository();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(() -> new ExpenseReportsViewModel(repository)))
                .get(ExpenseReportsViewModel.class);

        getBinding().errorView.setTitle(R.string.expense_reports_error_title);
        getBinding().errorView.setMessage(R.string.expense_reports_error_message);
        getBinding().errorView.setOnRetryListener(viewModel::refresh);

        viewModel.getReports().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    private void render(@NonNull Resource<List<ExpenseReport>> resource) {
        boolean hasData = !adapter.getCurrentList().isEmpty();
        getBinding().loadingView.setVisibility(resource.isLoading() && !hasData ? View.VISIBLE : View.GONE);
        getBinding().errorView.setVisibility(resource.isError() ? View.VISIBLE : View.GONE);

        if (resource.isSuccess() && resource.data != null) {
            adapter.submitList(resource.data);
            getBinding().textReportsEmpty.setVisibility(resource.data.isEmpty() ? View.VISIBLE : View.GONE);
        } else if (resource.isEmpty()) {
            adapter.submitList(Collections.emptyList());
            getBinding().textReportsEmpty.setVisibility(View.VISIBLE);
        } else {
            getBinding().textReportsEmpty.setVisibility(View.GONE);
        }
    }

    private void openReport(@NonNull ExpenseReport report) {
        Bundle args = new Bundle();
        args.putString(ARG_MONTH_KEY, report.getMonthKey());
        NavHostFragment.findNavController(this).navigate(R.id.action_expenseReports_to_reportDetail, args);
    }
}
