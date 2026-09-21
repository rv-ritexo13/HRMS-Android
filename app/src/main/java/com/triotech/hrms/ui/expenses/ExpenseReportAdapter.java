package com.triotech.hrms.ui.expenses;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.triotech.hrms.R;
import com.triotech.hrms.core.util.CurrencyUtils;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.data.model.ExpenseReport;
import com.triotech.hrms.databinding.ItemExpenseReportBinding;

/** One card per month report: month, item count, total and a status breakdown. */
public class ExpenseReportAdapter extends ListAdapter<ExpenseReport, ExpenseReportAdapter.ReportViewHolder> {

    @Nullable private OnReportClickListener listener;

    public interface OnReportClickListener {
        void onReportClick(@NonNull ExpenseReport report);
    }

    public ExpenseReportAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnReportClickListener(@Nullable OnReportClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ReportViewHolder(
                ItemExpenseReportBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        private final ItemExpenseReportBinding binding;

        ReportViewHolder(@NonNull ItemExpenseReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ExpenseReport report, @Nullable OnReportClickListener listener) {
            binding.textReportMonth.setText(DateUtils.formatMonthYear(report.getYear(), report.getMonth()));
            binding.textReportItemCount.setText(binding.getRoot().getResources().getQuantityString(
                    R.plurals.expense_report_item_count, report.getItemCount(), report.getItemCount()));
            binding.textReportTotal.setText(CurrencyUtils.formatRupees(report.getTotalAmount()));
            binding.textReportBreakdown.setText(
                    ExpensePresenter.reportStatusBreakdown(binding.getRoot().getContext(), report));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReportClick(report);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<ExpenseReport> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ExpenseReport>() {
                @Override
                public boolean areItemsTheSame(@NonNull ExpenseReport o, @NonNull ExpenseReport n) {
                    return o.getMonthKey().equals(n.getMonthKey());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ExpenseReport o, @NonNull ExpenseReport n) {
                    return o.getItemCount() == n.getItemCount() && o.getTotalAmount() == n.getTotalAmount()
                            && o.statusCounts().equals(n.statusCounts());
                }
            };
}
