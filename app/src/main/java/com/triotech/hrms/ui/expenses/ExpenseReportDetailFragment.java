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
import com.triotech.hrms.core.util.CurrencyUtils;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Expense;
import com.triotech.hrms.data.model.ExpenseReport;
import com.triotech.hrms.data.repository.ExpenseRepository;
import com.triotech.hrms.databinding.FragmentExpenseReportDetailBinding;

/**
 * A single month's expense report: a summary header (month, combined total, item
 * count, status breakdown) followed by every expense that rolls into it. Selecting
 * a line item opens its {@link ExpenseDetailFragment}.
 */
public class ExpenseReportDetailFragment extends BaseFragment<FragmentExpenseReportDetailBinding> {

    private ExpenseReportDetailViewModel viewModel;
    private final ExpenseAdapter adapter = new ExpenseAdapter();

    @Override
    protected FragmentExpenseReportDetailBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentExpenseReportDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        getBinding().toolbar.setNavigationOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());

        getBinding().recyclerReportExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().recyclerReportExpenses.setAdapter(adapter);
        adapter.setOnExpenseClickListener(this::openExpense);

        String monthKey = requireArguments().getString(ExpenseReportsFragment.ARG_MONTH_KEY, "");

        ExpenseRepository repository = ServiceLocator.getInstance().getExpenseRepository();
        viewModel = new ViewModelProvider(this,
                new ViewModelFactory(() -> new ExpenseReportDetailViewModel(repository, monthKey)))
                .get(ExpenseReportDetailViewModel.class);

        getBinding().errorView.setTitle(R.string.expense_reports_error_title);
        getBinding().errorView.setMessage(R.string.expense_reports_error_message);
        getBinding().errorView.setOnRetryListener(viewModel::refresh);

        viewModel.getReport().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    private void render(@NonNull Resource<ExpenseReport> resource) {
        boolean success = resource.isSuccess() && resource.data != null;
        getBinding().loadingView.setVisibility(resource.isLoading() && !success ? View.VISIBLE : View.GONE);
        getBinding().errorView.setVisibility(resource.isError() ? View.VISIBLE : View.GONE);
        getBinding().contentScroll.setVisibility(success ? View.VISIBLE : View.GONE);

        if (success) {
            bind(resource.data);
        }
    }

    private void bind(@NonNull ExpenseReport report) {
        String month = DateUtils.formatMonthYear(report.getYear(), report.getMonth());
        getBinding().toolbar.setTitle(month);
        getBinding().textReportMonth.setText(month);
        getBinding().textReportTotal.setText(CurrencyUtils.formatRupees(report.getTotalAmount()));
        getBinding().textReportItemCount.setText(getResources().getQuantityString(
                R.plurals.expense_report_item_count, report.getItemCount(), report.getItemCount()));
        getBinding().textReportBreakdown.setText(
                ExpensePresenter.reportStatusBreakdown(requireContext(), report));
        adapter.submitList(report.getExpenses());
    }

    private void openExpense(@NonNull Expense expense) {
        Bundle args = new Bundle();
        args.putString(ExpenseDetailFragment.ARG_EXPENSE_ID, expense.getId());
        NavHostFragment.findNavController(this).navigate(R.id.expenseDetailFragment, args);
    }
}
