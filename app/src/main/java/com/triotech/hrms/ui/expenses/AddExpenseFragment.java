package com.triotech.hrms.ui.expenses;

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
import com.triotech.hrms.data.model.Expense;
import com.triotech.hrms.data.model.ExpenseCategory;
import com.triotech.hrms.data.model.ExpenseStatus;
import com.triotech.hrms.data.repository.ExpenseRepository;
import com.triotech.hrms.databinding.FragmentAddExpenseBinding;

/**
 * Add Expense form. Validates title, category, a positive amount and a date
 * before creating an {@link Expense}. "Save Draft" stores it as DRAFT; "Submit"
 * stores it as SUBMITTED. An optional receipt can be picked (its display name is
 * recorded). On success it returns to My Expenses, which reloads to show it.
 * Mirrors {@code ApplyLeaveFragment}.
 */
public class AddExpenseFragment extends BaseFragment<FragmentAddExpenseBinding> {

    public static final String ARG_EXPENSE_ID = "expenseId";

    private AddExpenseViewModel viewModel;
    @Nullable private ExpenseCategory selectedCategory;
    private long dateMillis;
    @Nullable private String receiptName;

    /** Non-null when editing an existing Draft/Rejected expense; null when creating. */
    @Nullable private String editingId;
    private long editingCreatedMillis;

    private final ActivityResultLauncher<String> pickReceipt =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onReceiptPicked);

    @Override
    protected FragmentAddExpenseBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAddExpenseBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        getBinding().toolbar.setNavigationOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());

        ExpenseCategory[] categories = ExpenseCategory.values();
        String[] labels = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            labels[i] = getString(ExpensePresenter.labelFor(categories[i]));
        }
        getBinding().editCategory.setSimpleItems(labels);
        getBinding().editCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = categories[position];
            getBinding().layoutCategory.setError(null);
        });

        getBinding().editDate.setOnClickListener(v -> pickDate());
        getBinding().buttonReceipt.setOnClickListener(v -> pickReceipt.launch("*/*"));
        getBinding().buttonSaveDraft.setOnClickListener(v -> submit(ExpenseStatus.DRAFT));
        getBinding().buttonSubmit.setOnClickListener(v -> submit(ExpenseStatus.SUBMITTED));

        ExpenseRepository repository = ServiceLocator.getInstance().getExpenseRepository();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(() -> new AddExpenseViewModel(repository)))
                .get(AddExpenseViewModel.class);

        editingId = getArguments() != null ? getArguments().getString(ARG_EXPENSE_ID) : null;
        if (editingId != null) {
            getBinding().toolbar.setTitle(R.string.edit_expense_title);
            getBinding().loadingView.setVisibility(View.VISIBLE);
            viewModel.load(editingId).observe(getViewLifecycleOwner(), resource -> {
                if (resource.isSuccess() && resource.data != null) {
                    prefill(resource.data);
                    getBinding().loadingView.setVisibility(View.GONE);
                } else if (resource.isError()) {
                    getBinding().loadingView.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), R.string.expense_error_message, Toast.LENGTH_LONG).show();
                    NavHostFragment.findNavController(this).popBackStack();
                }
            });
        }
    }

    private void prefill(@NonNull Expense expense) {
        editingCreatedMillis = expense.getCreatedMillis();

        getBinding().editTitle.setText(expense.getTitle());

        selectedCategory = expense.getCategory();
        getBinding().editCategory.setText(getString(ExpensePresenter.labelFor(expense.getCategory())), false);

        getBinding().editAmount.setText(String.valueOf(expense.getAmount()));

        dateMillis = expense.getDateMillis();
        getBinding().editDate.setText(DateUtils.formatLongDate(expense.getDateMillis()));

        getBinding().editDescription.setText(expense.getDescription());

        if (expense.hasReceipt()) {
            receiptName = expense.getReceiptName();
            getBinding().textReceiptName.setText(receiptName);
        }
    }

    private void pickDate() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.add_expense_hint_date)
                .setSelection(dateMillis > 0 ? dateMillis : MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            dateMillis = selection;
            getBinding().editDate.setText(DateUtils.formatLongDate(selection));
            getBinding().layoutDate.setError(null);
        });
        picker.show(getChildFragmentManager(), "expense_date_picker");
    }

    private void onReceiptPicked(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        receiptName = resolveDisplayName(uri);
        getBinding().textReceiptName.setText(
                receiptName != null ? receiptName : getString(R.string.add_expense_receipt_none));
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

    private void submit(@NonNull ExpenseStatus status) {
        boolean valid = true;

        String title = getBinding().editTitle.getText() == null
                ? "" : getBinding().editTitle.getText().toString().trim();
        if (title.isEmpty()) {
            getBinding().layoutTitle.setError(getString(R.string.add_expense_err_title));
            valid = false;
        } else {
            getBinding().layoutTitle.setError(null);
        }

        if (selectedCategory == null) {
            getBinding().layoutCategory.setError(getString(R.string.add_expense_err_category));
            valid = false;
        }

        long amount = 0L;
        String amountText = getBinding().editAmount.getText() == null
                ? "" : getBinding().editAmount.getText().toString().trim();
        try {
            amount = amountText.isEmpty() ? 0L : Long.parseLong(amountText);
        } catch (NumberFormatException e) {
            amount = 0L;
        }
        if (amount <= 0L) {
            getBinding().layoutAmount.setError(getString(R.string.add_expense_err_amount));
            valid = false;
        } else {
            getBinding().layoutAmount.setError(null);
        }

        if (dateMillis <= 0L) {
            getBinding().layoutDate.setError(getString(R.string.add_expense_err_date));
            valid = false;
        }

        if (!valid || selectedCategory == null) {
            return;
        }

        String description = getBinding().editDescription.getText() == null
                ? "" : getBinding().editDescription.getText().toString().trim();

        boolean editing = editingId != null;
        Expense expense = new Expense(
                editing ? editingId : "EXP-" + System.currentTimeMillis(),
                title,
                selectedCategory,
                amount,
                dateMillis,
                description,
                status,
                editing ? editingCreatedMillis : System.currentTimeMillis(),
                receiptName);

        setButtonsEnabled(false);
        getBinding().loadingView.setVisibility(View.VISIBLE);
        int successMessage = status == ExpenseStatus.DRAFT
                ? R.string.add_expense_saved_draft : R.string.add_expense_submitted;
        (editing ? viewModel.update(expense) : viewModel.save(expense))
                .observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess()) {
                Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            } else if (resource.isError()) {
                getBinding().loadingView.setVisibility(View.GONE);
                setButtonsEnabled(true);
                Toast.makeText(requireContext(), R.string.add_expense_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        getBinding().buttonSaveDraft.setEnabled(enabled);
        getBinding().buttonSubmit.setEnabled(enabled);
    }
}
