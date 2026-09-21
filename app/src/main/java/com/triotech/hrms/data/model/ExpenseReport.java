package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A calendar month's expenses rolled up into a single report. Derived on demand
 * from the {@code expenses} table (grouped by expense date) rather than persisted
 * — there is no separate reports table, so a report always reflects the current
 * state of its underlying expenses.
 */
public final class ExpenseReport {

    private final String monthKey; // "yyyy-MM"
    private final int year;
    private final int month; // 1-12
    private final List<Expense> expenses;
    private final long totalAmount;

    public ExpenseReport(
            @NonNull String monthKey, int year, int month, @NonNull List<Expense> expenses) {
        this.monthKey = monthKey;
        this.year = year;
        this.month = month;
        this.expenses = Collections.unmodifiableList(expenses);
        long sum = 0L;
        for (Expense e : expenses) {
            sum += e.getAmount();
        }
        this.totalAmount = sum;
    }

    @NonNull
    public String getMonthKey() {
        return monthKey;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    @NonNull
    public List<Expense> getExpenses() {
        return expenses;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public int getItemCount() {
        return expenses.size();
    }

    /** Count of expenses per status (only non-zero entries), in {@link ExpenseStatus} order. */
    @NonNull
    public Map<ExpenseStatus, Integer> statusCounts() {
        Map<ExpenseStatus, Integer> counts = new LinkedHashMap<>();
        for (ExpenseStatus status : ExpenseStatus.values()) {
            int count = 0;
            for (Expense e : expenses) {
                if (e.getStatus() == status) {
                    count++;
                }
            }
            if (count > 0) {
                counts.put(status, count);
            }
        }
        return counts;
    }
}
