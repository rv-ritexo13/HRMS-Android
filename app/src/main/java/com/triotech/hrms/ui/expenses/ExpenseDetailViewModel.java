package com.triotech.hrms.ui.expenses;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Expense;
import com.triotech.hrms.data.repository.ExpenseRepository;

/**
 * Backs {@link ExpenseDetailFragment}: loads a single expense by id.
 */
public class ExpenseDetailViewModel extends BaseViewModel {

    private final ExpenseRepository repository;
    private final String expenseId;
    private final MediatorLiveData<Resource<Expense>> expense = new MediatorLiveData<>();
    @Nullable private LiveData<Resource<Expense>> currentSource;

    public ExpenseDetailViewModel(@NonNull ExpenseRepository repository, @NonNull String expenseId) {
        this.repository = repository;
        this.expenseId = expenseId;
        load();
    }

    @NonNull
    public LiveData<Resource<Expense>> getExpense() {
        return expense;
    }

    public void retry() {
        load();
    }

    @NonNull
    public LiveData<Resource<Boolean>> submit() {
        return repository.submitExpense(expenseId);
    }

    private void load() {
        if (currentSource != null) {
            expense.removeSource(currentSource);
        }
        currentSource = repository.observeExpense(expenseId);
        expense.addSource(currentSource, expense::setValue);
    }
}
