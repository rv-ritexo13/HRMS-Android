package com.triotech.hrms.ui.salary;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.triotech.hrms.core.util.CurrencyUtils;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.data.model.Payslip;
import com.triotech.hrms.databinding.ItemSalaryHistoryBinding;

/**
 * RecyclerView adapter for the salary history list. Uses {@link ListAdapter} so
 * the newest-first list is diffed on refresh instead of a blanket redraw.
 * Selecting a row asks the host to show that month's details.
 */
public class SalaryHistoryAdapter extends ListAdapter<Payslip, SalaryHistoryAdapter.HistoryViewHolder> {

    @Nullable private OnMonthClickListener listener;

    public interface OnMonthClickListener {
        void onMonthClick(@NonNull Payslip payslip);
    }

    public SalaryHistoryAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnMonthClickListener(@Nullable OnMonthClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSalaryHistoryBinding binding = ItemSalaryHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final ItemSalaryHistoryBinding binding;

        HistoryViewHolder(@NonNull ItemSalaryHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Payslip payslip, @Nullable OnMonthClickListener listener) {
            binding.textHistoryMonth.setText(
                    DateUtils.formatMonthYear(payslip.getYear(), payslip.getMonth()));
            String creditedPrefix = payslip.isCredited() ? "Credited " : "Due ";
            binding.textHistoryCredited.setText(
                    creditedPrefix + DateUtils.formatLongDate(payslip.getCreditDateMillis()));
            binding.textHistoryNet.setText(CurrencyUtils.formatRupees(payslip.getNetSalary()));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMonthClick(payslip);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<Payslip> DIFF_CALLBACK = new DiffUtil.ItemCallback<Payslip>() {
        @Override
        public boolean areItemsTheSame(@NonNull Payslip oldItem, @NonNull Payslip newItem) {
            return oldItem.getMonthKey().equals(newItem.getMonthKey());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Payslip oldItem, @NonNull Payslip newItem) {
            return oldItem.getNetSalary() == newItem.getNetSalary()
                    && oldItem.getCreditDateMillis() == newItem.getCreditDateMillis()
                    && oldItem.isCredited() == newItem.isCredited();
        }
    };
}
