package com.triotech.hrms.ui.leave;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.triotech.hrms.R;
import com.triotech.hrms.data.model.LeaveBalance;
import com.triotech.hrms.databinding.ItemLeaveBalanceBinding;

/**
 * Horizontal list of leave-balance cards, one per leave type, each with an accent
 * icon, the available days and a used/total progress indicator.
 */
public class LeaveBalanceAdapter extends ListAdapter<LeaveBalance, LeaveBalanceAdapter.BalanceViewHolder> {

    public LeaveBalanceAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public BalanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BalanceViewHolder(
                ItemLeaveBalanceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BalanceViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class BalanceViewHolder extends RecyclerView.ViewHolder {
        private final ItemLeaveBalanceBinding binding;

        BalanceViewHolder(@NonNull ItemLeaveBalanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull LeaveBalance balance) {
            int accent = ContextCompat.getColor(
                    binding.getRoot().getContext(), LeavePresenter.accentColorFor(balance.getType()));

            binding.imageBalanceIcon.setImageResource(LeavePresenter.iconFor(balance.getType()));
            binding.imageBalanceIcon.setImageTintList(ColorStateList.valueOf(accent));
            binding.textBalanceType.setText(LeavePresenter.labelFor(balance.getType()));
            binding.textBalanceAvailable.setText(binding.getRoot().getContext().getString(
                    R.string.leave_balance_available_format, balance.getAvailableDays()));
            binding.progressBalance.setIndicatorColor(accent);
            binding.progressBalance.setProgressCompat(balance.getUsedPercent(), false);
            binding.textBalanceUsed.setText(binding.getRoot().getContext().getString(
                    R.string.leave_balance_used_format, balance.getUsedDays(), balance.getTotalDays()));
        }
    }

    private static final DiffUtil.ItemCallback<LeaveBalance> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<LeaveBalance>() {
                @Override
                public boolean areItemsTheSame(@NonNull LeaveBalance o, @NonNull LeaveBalance n) {
                    return o.getType() == n.getType();
                }

                @Override
                public boolean areContentsTheSame(@NonNull LeaveBalance o, @NonNull LeaveBalance n) {
                    return o.getTotalDays() == n.getTotalDays() && o.getUsedDays() == n.getUsedDays();
                }
            };
}
