package com.triotech.hrms.ui.expenses;

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
import com.triotech.hrms.core.util.CurrencyUtils;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Expense;
import com.triotech.hrms.data.model.ExpenseStatus;
import com.triotech.hrms.data.repository.ExpenseRepository;
import com.triotech.hrms.databinding.FragmentExpenseDetailBinding;
import com.triotech.hrms.databinding.ItemProfileFieldBinding;

/**
 * Full detail for a single expense. Shows the expense ID, title, category,
 * amount, date, description, receipt and status. Read-only in Step 1 (approval
 * actions arrive in a later phase). Mirrors {@code LeaveDetailFragment}.
 */
public class ExpenseDetailFragment extends BaseFragment<FragmentExpenseDetailBinding> {

    public static final String ARG_EXPENSE_ID = "expenseId";

    private ExpenseDetailViewModel viewModel;

    @Override
    protected FragmentExpenseDetailBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentExpenseDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        String expenseId = getArguments() != null ? getArguments().getString(ARG_EXPENSE_ID) : null;
        if (expenseId == null) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        getBinding().toolbar.setNavigationOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());

        getBinding().errorView.setTitle(R.string.expense_error_title);
        getBinding().errorView.setMessage(R.string.expense_error_message);
        getBinding().errorView.setOnRetryListener(() -> viewModel.retry());

        ExpenseRepository repository = ServiceLocator.getInstance().getExpenseRepository();
        viewModel = new ViewModelProvider(this,
                new ViewModelFactory(() -> new ExpenseDetailViewModel(repository, expenseId)))
                .get(ExpenseDetailViewModel.class);

        viewModel.getExpense().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.retry();
        }
    }

    private void render(@NonNull Resource<Expense> resource) {
        getBinding().loadingView.setVisibility(resource.isLoading() ? View.VISIBLE : View.GONE);
        getBinding().errorView.setVisibility(resource.isError() ? View.VISIBLE : View.GONE);
        getBinding().contentContainer.setVisibility(resource.isSuccess() ? View.VISIBLE : View.GONE);
        if (resource.isSuccess() && resource.data != null) {
            bind(resource.data);
        }
    }

    private void bind(@NonNull Expense e) {
        getBinding().imageCategoryIcon.setImageResource(ExpensePresenter.iconFor(e.getCategory()));
        getBinding().imageCategoryIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), ExpensePresenter.accentColorFor(e.getCategory()))));
        getBinding().textTitle.setText(e.getTitle());

        getBinding().chipStatus.setText(ExpensePresenter.labelFor(e.getStatus()));
        getBinding().chipStatus.setTextColor(
                ContextCompat.getColor(requireContext(), ExpensePresenter.statusTextColor(e.getStatus())));
        getBinding().chipStatus.setChipBackgroundColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), ExpensePresenter.statusContainerColor(e.getStatus()))));

        field(getBinding().dId, R.drawable.ic_badge, R.string.expense_detail_id, e.getId());
        field(getBinding().dCategory, ExpensePresenter.iconFor(e.getCategory()),
                R.string.expense_detail_category, getString(ExpensePresenter.labelFor(e.getCategory())));
        field(getBinding().dAmount, R.drawable.ic_salary, R.string.expense_detail_amount,
                CurrencyUtils.formatRupees(e.getAmount()));
        field(getBinding().dDate, R.drawable.ic_calendar, R.string.expense_detail_date,
                DateUtils.formatLongDate(e.getDateMillis()));
        field(getBinding().dDescription, R.drawable.ic_description, R.string.expense_detail_description,
                e.hasDescription() ? e.getDescription() : getString(R.string.expense_detail_no_description));

        field(getBinding().dReceipt, R.drawable.ic_attach_file, R.string.expense_detail_receipt,
                e.hasReceipt() ? e.getReceiptName() : getString(R.string.expense_detail_no_receipt));

        bindActions(e);
    }

    private void bindActions(@NonNull Expense e) {
        boolean editable = e.getStatus() == ExpenseStatus.DRAFT || e.getStatus() == ExpenseStatus.REJECTED;
        boolean submittable = e.getStatus() == ExpenseStatus.DRAFT;

        getBinding().layoutActions.setVisibility(editable || submittable ? View.VISIBLE : View.GONE);
        getBinding().buttonEdit.setVisibility(editable ? View.VISIBLE : View.GONE);
        getBinding().buttonSubmit.setVisibility(submittable ? View.VISIBLE : View.GONE);

        getBinding().buttonEdit.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(AddExpenseFragment.ARG_EXPENSE_ID, e.getId());
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_expenseDetail_to_editExpense, args);
        });
        getBinding().buttonSubmit.setOnClickListener(v -> submit());
    }

    private void submit() {
        getBinding().loadingView.setVisibility(View.VISIBLE);
        viewModel.submit().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess()) {
                Toast.makeText(requireContext(), R.string.expense_detail_submitted, Toast.LENGTH_SHORT).show();
                viewModel.retry();
            } else if (resource.isError()) {
                getBinding().loadingView.setVisibility(View.GONE);
                Toast.makeText(requireContext(), R.string.expense_detail_submit_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void field(
            @NonNull ItemProfileFieldBinding row, @DrawableRes int icon, int labelRes, @NonNull String value) {
        row.imageFieldIcon.setImageResource(icon);
        row.textFieldLabel.setText(labelRes);
        row.textFieldValue.setText(value);
    }
}
