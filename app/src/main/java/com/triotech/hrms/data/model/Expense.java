package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A single employee expense claim and its lifecycle. Persisted in the
 * {@code expenses} table of {@link com.triotech.hrms.data.local.HrmsDatabase}.
 *
 * <p>Amount is stored as whole rupees (a {@code long}) to match the rest of the
 * app's money handling and {@code CurrencyUtils.formatRupees(long)}.</p>
 */
public final class Expense {

    private final String id;
    private final String title;
    private final ExpenseCategory category;
    private final long amount;
    private final long dateMillis;
    private final String description;
    private final ExpenseStatus status;
    private final long createdMillis;
    @Nullable private final String receiptName;

    public Expense(
            @NonNull String id,
            @NonNull String title,
            @NonNull ExpenseCategory category,
            long amount,
            long dateMillis,
            @NonNull String description,
            @NonNull ExpenseStatus status,
            long createdMillis,
            @Nullable String receiptName) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.dateMillis = dateMillis;
        this.description = description;
        this.status = status;
        this.createdMillis = createdMillis;
        this.receiptName = receiptName;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public ExpenseCategory getCategory() {
        return category;
    }

    public long getAmount() {
        return amount;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @NonNull
    public ExpenseStatus getStatus() {
        return status;
    }

    public long getCreatedMillis() {
        return createdMillis;
    }

    @Nullable
    public String getReceiptName() {
        return receiptName;
    }

    public boolean hasDescription() {
        return !description.trim().isEmpty();
    }

    public boolean hasReceipt() {
        return receiptName != null && !receiptName.trim().isEmpty();
    }
}
