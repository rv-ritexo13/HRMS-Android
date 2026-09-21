package com.triotech.hrms.ui.leave;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.triotech.hrms.R;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.data.model.LeaveRequest;
import com.triotech.hrms.databinding.ItemLeaveRequestBinding;

/**
 * Vertical list of leave requests with a coloured status chip. Selecting a row
 * opens the request's detail screen.
 */
public class LeaveRequestAdapter extends ListAdapter<LeaveRequest, LeaveRequestAdapter.RequestViewHolder> {

    @Nullable private OnRequestClickListener listener;

    public interface OnRequestClickListener {
        void onRequestClick(@NonNull LeaveRequest request);
    }

    public LeaveRequestAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnRequestClickListener(@Nullable OnRequestClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RequestViewHolder(
                ItemLeaveRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        private final ItemLeaveRequestBinding binding;

        RequestViewHolder(@NonNull ItemLeaveRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull LeaveRequest request, @Nullable OnRequestClickListener listener) {
            int accent = ContextCompat.getColor(
                    binding.getRoot().getContext(), LeavePresenter.accentColorFor(request.getType()));
            binding.imageRequestIcon.setImageResource(LeavePresenter.iconFor(request.getType()));
            binding.imageRequestIcon.setImageTintList(ColorStateList.valueOf(accent));
            binding.textRequestType.setText(LeavePresenter.labelFor(request.getType()));

            binding.textRequestDates.setText(
                    DateUtils.formatLongDate(request.getStartMillis()) + " - "
                            + DateUtils.formatLongDate(request.getEndMillis()));
            binding.textRequestDays.setText(binding.getRoot().getContext().getString(
                    R.string.leave_days_format, request.getDays()));
            binding.textRequestReason.setText(request.getReason());

            binding.chipStatus.setText(LeavePresenter.labelFor(request.getStatus()));
            binding.chipStatus.setTextColor(ContextCompat.getColor(
                    binding.getRoot().getContext(), LeavePresenter.statusTextColor(request.getStatus())));
            binding.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(
                    binding.getRoot().getContext(), LeavePresenter.statusContainerColor(request.getStatus()))));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRequestClick(request);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<LeaveRequest> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<LeaveRequest>() {
                @Override
                public boolean areItemsTheSame(@NonNull LeaveRequest o, @NonNull LeaveRequest n) {
                    return o.getId().equals(n.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull LeaveRequest o, @NonNull LeaveRequest n) {
                    return o.getStatus() == n.getStatus() && o.getDays() == n.getDays()
                            && o.getStartMillis() == n.getStartMillis() && o.getEndMillis() == n.getEndMillis()
                            && o.getReason().equals(n.getReason());
                }
            };
}
