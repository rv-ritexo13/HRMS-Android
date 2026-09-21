package com.triotech.hrms.ui.expenses;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Expense;
import com.triotech.hrms.data.repository.ExpenseRepository;

/**
 * Backs {@link AddExpenseFragment}: saves a new expense (as a DRAFT or a
 * SUBMITTED claim) and exposes the one-shot loading/success/error stream.
 */
public class AddExpenseViewModel extends BaseViewModel {

    private final ExpenseRepository repository;

    public AddExpenseViewModel(@NonNull ExpenseRepository repository) {
        this.repository = repository;
    }

    @NonNull
    public LiveData<Resource<Expense>> save(@NonNull Expense expense) {
        return repository.saveExpense(expense);
    }

    /** Loads an existing expense so the form can be prefilled for editing. */
    @NonNull
    public LiveData<Resource<Expense>> load(@NonNull String id) {
        return repository.observeExpense(id);
    }

    @NonNull
    public LiveData<Resource<Expense>> update(@NonNull Expense expense) {
        return repository.updateExpense(expense);
    }
}
