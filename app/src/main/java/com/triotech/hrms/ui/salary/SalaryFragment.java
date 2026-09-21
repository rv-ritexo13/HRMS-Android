package com.triotech.hrms.ui.salary;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.triotech.hrms.R;
import com.triotech.hrms.core.base.BaseFragment;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.CurrencyUtils;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Payslip;
import com.triotech.hrms.data.repository.SalaryRepository;
import com.triotech.hrms.databinding.FragmentSalaryBinding;
import com.triotech.hrms.databinding.ItemSalaryComponentBinding;
import java.util.List;

/**
 * Salary &amp; Payslip dashboard. Loads salary history from the local SQLite
 * database and drives the shared Loading/Empty/Error/Success state components off
 * one {@code Resource} stream. The summary + earnings/deductions breakdown at the
 * top reflect the selected month; tapping a history row re-selects that month, and
 * "View Payslip" opens the full payslip detail screen.
 */
public class SalaryFragment extends BaseFragment<FragmentSalaryBinding> {

    static final String ARG_MONTH_KEY = "monthKey";

    private SalaryViewModel viewModel;
    private final SalaryHistoryAdapter adapter = new SalaryHistoryAdapter();
    @Nullable private String selectedMonthKey;

    @Override
    protected FragmentSalaryBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentSalaryBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        SalaryRepository repository = ServiceLocator.getInstance().getSalaryRepository();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(() -> new SalaryViewModel(repository)))
                .get(SalaryViewModel.class);

        getBinding().recyclerSalaryHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().recyclerSalaryHistory.setAdapter(adapter);
        adapter.setOnMonthClickListener(payslip -> {
            viewModel.selectMonth(payslip.getMonthKey());
            getBinding().scrollContent.smoothScrollTo(0, 0);
        });

        getBinding().emptyStateView.setIcon(R.drawable.ic_salary);
        getBinding().emptyStateView.setTitle(R.string.salary_empty_title);
        getBinding().emptyStateView.setMessage(R.string.salary_empty_message);
        getBinding().errorView.setTitle(R.string.salary_error_title);
        getBinding().errorView.setMessage(R.string.salary_error_message);
        getBinding().errorView.setOnRetryListener(viewModel::retry);
        getBinding().swipeRefresh.setOnRefreshListener(viewModel::retry);

        getBinding().buttonViewPayslip.setOnClickListener(v -> openPayslip());

        viewModel.getHistory().observe(getViewLifecycleOwner(), this::renderHistory);
        viewModel.getSelectedPayslip().observe(getViewLifecycleOwner(), this::renderSelected);
    }

    private void renderHistory(@NonNull Resource<List<Payslip>> resource) {
        boolean hasData = !adapter.getCurrentList().isEmpty();
        getBinding().swipeRefresh.setRefreshing(resource.isLoading() && hasData);

        boolean showFullScreenLoading = resource.isLoading() && !hasData;
        getBinding().loadingView.setVisibility(showFullScreenLoading ? View.VISIBLE : View.GONE);
        getBinding().errorView.setVisibility(resource.isError() ? View.VISIBLE : View.GONE);
        getBinding().emptyStateView.setVisibility(resource.isEmpty() ? View.VISIBLE : View.GONE);
        getBinding().swipeRefresh.setVisibility(resource.isSuccess() ? View.VISIBLE : View.GONE);

        if (resource.isSuccess() && resource.data != null) {
            adapter.submitList(resource.data);
        }
    }

    private void renderSelected(@Nullable Payslip payslip) {
        if (payslip == null) {
            return;
        }
        selectedMonthKey = payslip.getMonthKey();

        getBinding().textSelectedMonth.setText(
                DateUtils.formatMonthYear(payslip.getYear(), payslip.getMonth()));
        getBinding().textGrossValue.setText(CurrencyUtils.formatRupees(payslip.getGrossSalary()));
        getBinding().textDeductionsValue.setText(
                "- " + CurrencyUtils.formatRupees(payslip.getTotalDeductions()));
        getBinding().textNetValue.setText(CurrencyUtils.formatRupees(payslip.getNetSalary()));

        getBinding().textCreditStatus.setText(
                payslip.isCredited() ? R.string.salary_credited_on : R.string.salary_pending);
        getBinding().textCreditDate.setText(DateUtils.formatLongDate(payslip.getCreditDateMillis()));

        // Earnings breakdown
        bindRow(getBinding().rowBasic, R.string.salary_component_basic, payslip.getBasic(), false);
        bindRow(getBinding().rowHra, R.string.salary_component_hra, payslip.getHra(), false);
        bindRow(getBinding().rowSpecial, R.string.salary_component_special, payslip.getSpecialAllowance(), false);
        bindRow(getBinding().rowOtherAllowances, R.string.salary_component_other_allowances,
                payslip.getOtherAllowances(), false);
        bindRow(getBinding().rowGrossTotal, R.string.salary_gross_total, payslip.getGrossSalary(), true);

        // Deductions breakdown
        bindRow(getBinding().rowPf, R.string.salary_component_pf, payslip.getProvidentFund(), false);
        bindRow(getBinding().rowPtax, R.string.salary_component_ptax, payslip.getProfessionalTax(), false);
        bindRow(getBinding().rowOtherDeductions, R.string.salary_component_other_deductions,
                payslip.getOtherDeductions(), false);
        bindRow(getBinding().rowDeductionsTotal, R.string.salary_deductions_total,
                payslip.getTotalDeductions(), true);
    }

    private void bindRow(
            @NonNull ItemSalaryComponentBinding row, int labelRes, long amount, boolean emphasise) {
        row.textComponentLabel.setText(labelRes);
        row.textComponentAmount.setText(CurrencyUtils.formatRupees(amount));
        int style = emphasise ? Typeface.BOLD : Typeface.NORMAL;
        row.textComponentLabel.setTypeface(null, style);
        row.textComponentAmount.setTypeface(null, style);
    }

    private void openPayslip() {
        if (selectedMonthKey == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putString(ARG_MONTH_KEY, selectedMonthKey);
        NavHostFragment.findNavController(this).navigate(R.id.action_salary_to_payslip, args);
    }
}
