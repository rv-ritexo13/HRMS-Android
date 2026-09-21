package com.triotech.hrms.ui.expenses;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.triotech.hrms.core.util.CurrencyUtils;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.data.model.Expense;
import com.triotech.hrms.databinding.ItemExpenseBinding;

/**
 * Vertical list of expenses with a coloured status chip. Selecting a row opens
 * the expense's detail screen. Mirrors {@code LeaveRequestAdapter}.
 */
public class ExpenseAdapter extends ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder> {

    @Nullable private OnExpenseClickListener listener;

    public interface OnExpenseClickListener {
        void onExpenseClick(@NonNull Expense expense);
    }

    public ExpenseAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnExpenseClickListener(@Nullable OnExpenseClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ExpenseViewHolder(
                ItemExpenseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final ItemExpenseBinding binding;

        ExpenseViewHolder(@NonNull ItemExpenseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Expense expense, @Nullable OnExpenseClickListener listener) {
            int accent = ContextCompat.getColor(
                    binding.getRoot().getContext(), ExpensePresenter.accentColorFor(expense.getCategory()));
            binding.imageExpenseIcon.setImageResource(ExpensePresenter.iconFor(expense.getCategory()));
            binding.imageExpenseIcon.setImageTintList(ColorStateList.valueOf(accent));

            binding.textExpenseTitle.setText(expense.getTitle());
            binding.textExpenseCategoryDate.setText(
                    binding.getRoot().getContext().getString(ExpensePresenter.labelFor(expense.getCategory()))
                            + " · " + DateUtils.formatLongDate(expense.getDateMillis()));
            binding.textExpenseAmount.setText(CurrencyUtils.formatRupees(expense.getAmount()));

            binding.chipStatus.setText(ExpensePresenter.labelFor(expense.getStatus()));
            binding.chipStatus.setTextColor(ContextCompat.getColor(
                    binding.getRoot().getContext(), ExpensePresenter.statusTextColor(expense.getStatus())));
            binding.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(
                    binding.getRoot().getContext(), ExpensePresenter.statusContainerColor(expense.getStatus()))));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onExpenseClick(expense);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<Expense> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Expense>() {
                @Override
                public boolean areItemsTheSame(@NonNull Expense o, @NonNull Expense n) {
                    return o.getId().equals(n.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Expense o, @NonNull Expense n) {
                    return o.getStatus() == n.getStatus() && o.getAmount() == n.getAmount()
                            && o.getCategory() == n.getCategory() && o.getDateMillis() == n.getDateMillis()
                            && o.getTitle().equals(n.getTitle());
                }
            };
}
