package com.triotech.hrms.ui.attendance;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.triotech.hrms.R;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.data.model.AttendanceHistoryEntry;
import com.triotech.hrms.databinding.ItemAttendanceHistoryBinding;

/** Feeds the Attendance screen's "Date | Check-in | Check-out | Hours | Status" history table. */
public class AttendanceHistoryAdapter extends ListAdapter<AttendanceHistoryEntry, AttendanceHistoryAdapter.ViewHolder> {

    public AttendanceHistoryAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAttendanceHistoryBinding binding =
                ItemAttendanceHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAttendanceHistoryBinding binding;

        ViewHolder(@NonNull ItemAttendanceHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull AttendanceHistoryEntry entry) {
            android.content.Context context = itemView.getContext();
            String placeholder = context.getString(R.string.attendance_value_placeholder);

            binding.textHistoryDate.setText(DateUtils.formatShortDate(entry.getDateMillis()));
            binding.textHistoryCheckIn.setText(
                    entry.getCheckInTimeMillis() != null ? DateUtils.formatTime(entry.getCheckInTimeMillis()) : placeholder);
            binding.textHistoryCheckOut.setText(
                    entry.getCheckOutTimeMillis() != null ? DateUtils.formatTime(entry.getCheckOutTimeMillis()) : placeholder);
            binding.textHistoryHours.setText(entry.getCheckInTimeMillis() != null && entry.getCheckOutTimeMillis() != null
                    ? DateUtils.formatDuration(entry.getWorkingDurationMillis())
                    : placeholder);

            binding.chipHistoryStatus.setText(AttendanceStatusPresenter.labelRes(entry.getStatus()));
            int textColor = ContextCompat.getColor(context, AttendanceStatusPresenter.colorRes(entry.getStatus()));
            int bgColor = ContextCompat.getColor(context, AttendanceStatusPresenter.containerColorRes(entry.getStatus()));
            binding.chipHistoryStatus.setTextColor(textColor);
            binding.chipHistoryStatus.setChipBackgroundColor(ColorStateList.valueOf(bgColor));
        }
    }

    private static final DiffUtil.ItemCallback<AttendanceHistoryEntry> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AttendanceHistoryEntry>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AttendanceHistoryEntry oldItem, @NonNull AttendanceHistoryEntry newItem) {
                    return oldItem.getDateKey().equals(newItem.getDateKey());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AttendanceHistoryEntry oldItem, @NonNull AttendanceHistoryEntry newItem) {
                    return oldItem.getStatus() == newItem.getStatus()
                            && java.util.Objects.equals(oldItem.getCheckInTimeMillis(), newItem.getCheckInTimeMillis())
                            && java.util.Objects.equals(oldItem.getCheckOutTimeMillis(), newItem.getCheckOutTimeMillis());
                }
            };
}
