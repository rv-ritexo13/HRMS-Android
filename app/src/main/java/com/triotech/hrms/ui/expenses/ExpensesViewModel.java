package com.triotech.hrms.ui.expenses;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Expense;
import com.triotech.hrms.data.repository.ExpenseRepository;
import java.util.List;

/**
 * Backs {@link ExpensesFragment}: exposes the expense list as a {@code Resource}
 * stream and reloads it on demand (after returning from Add Expense).
 */
public class ExpensesViewModel extends BaseViewModel {

    private final ExpenseRepository repository;

    private final MediatorLiveData<Resource<List<Expense>>> expenses = new MediatorLiveData<>();
    @Nullable private LiveData<Resource<List<Expense>>> expensesSource;

    public ExpensesViewModel(@NonNull ExpenseRepository repository) {
        this.repository = repository;
        refresh();
    }

    @NonNull
    public LiveData<Resource<List<Expense>>> getExpenses() {
        return expenses;
    }

    public void refresh() {
        if (expensesSource != null) {
            expenses.removeSource(expensesSource);
        }
        expensesSource = repository.observeExpenses();
        expenses.addSource(expensesSource, expenses::setValue);
    }
}
