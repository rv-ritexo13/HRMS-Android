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
import com.triotech.hrms.data.model.Expense;
import com.triotech.hrms.data.repository.ExpenseRepository;
import com.triotech.hrms.databinding.FragmentExpensesBinding;
import java.util.Collections;
import java.util.List;

/**
 * My Expenses: the expense list plus an Add Expense action. Reloads on resume so
 * a newly saved expense is reflected immediately. Mirrors {@code LeaveFragment}.
 */
public class ExpensesFragment extends BaseFragment<FragmentExpensesBinding> {

    private ExpensesViewModel viewModel;
    private final ExpenseAdapter adapter = new ExpenseAdapter();

    @Override
    protected FragmentExpensesBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentExpensesBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        getBinding().toolbar.setNavigationOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());

        getBinding().recyclerExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().recyclerExpenses.setAdapter(adapter);
        adapter.setOnExpenseClickListener(this::openDetail);

        getBinding().buttonAddExpense.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_expenses_to_addExpense));
        getBinding().buttonMonthlyReports.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_expenses_to_reports));

        ExpenseRepository repository = ServiceLocator.getInstance().getExpenseRepository();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(() -> new ExpensesViewModel(repository)))
                .get(ExpensesViewModel.class);

        getBinding().errorView.setTitle(R.string.expense_error_title);
        getBinding().errorView.setMessage(R.string.expense_error_message);
        getBinding().errorView.setOnRetryListener(viewModel::refresh);

        viewModel.getExpenses().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    private void render(@NonNull Resource<List<Expense>> resource) {
        boolean hasData = !adapter.getCurrentList().isEmpty();
        getBinding().loadingView.setVisibility(resource.isLoading() && !hasData ? View.VISIBLE : View.GONE);
        getBinding().errorView.setVisibility(resource.isError() ? View.VISIBLE : View.GONE);

        if (resource.isSuccess() && resource.data != null) {
            adapter.submitList(resource.data);
            getBinding().textExpensesEmpty.setVisibility(resource.data.isEmpty() ? View.VISIBLE : View.GONE);
        } else if (resource.isEmpty()) {
            adapter.submitList(Collections.emptyList());
            getBinding().textExpensesEmpty.setVisibility(View.VISIBLE);
        } else {
            getBinding().textExpensesEmpty.setVisibility(View.GONE);
        }
    }

    private void openDetail(@NonNull Expense expense) {
        Bundle args = new Bundle();
        args.putString(ExpenseDetailFragment.ARG_EXPENSE_ID, expense.getId());
        NavHostFragment.findNavController(this).navigate(R.id.action_expenses_to_expenseDetail, args);
    }
}
