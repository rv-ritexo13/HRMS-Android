package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Expense;
import java.util.List;

/**
 * Data contract for the Expense module. Backed by {@link DbExpenseRepository}
 * reading and writing the seeded {@code expenses} table, so saved drafts and
 * submitted claims persist. A real expense API can replace it behind
 * {@link com.triotech.hrms.core.di.ServiceLocator} with no UI change — the same
 * approach the Leave module uses.
 */
public interface ExpenseRepository {

    /** All expenses, most recent (by expense date) first. */
    @NonNull
    LiveData<Resource<List<Expense>>> observeExpenses();

    /** A single expense by id, or an error if not found. */
    @NonNull
    LiveData<Resource<Expense>> observeExpense(@NonNull String id);

    /**
     * Inserts a new expense with whatever status it carries (DRAFT for "Save
     * Draft", SUBMITTED for "Submit"). Emits loading, then success with the
     * saved expense.
     */
    @NonNull
    LiveData<Resource<Expense>> saveExpense(@NonNull Expense expense);

    /**
     * Updates an existing expense (matched by id) in place — used when editing a
     * Draft or Rejected expense. Emits loading, then success with the saved
     * expense.
     */
    @NonNull
    LiveData<Resource<Expense>> updateExpense(@NonNull Expense expense);

    /**
     * Moves a DRAFT expense to SUBMITTED. Guarded so only drafts can be
     * submitted. Emits loading, then success or error.
     */
    @NonNull
    LiveData<Resource<Boolean>> submitExpense(@NonNull String id);
}
