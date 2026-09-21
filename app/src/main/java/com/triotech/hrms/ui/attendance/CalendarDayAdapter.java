package com.triotech.hrms.ui.attendance;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.MaterialColors;
import com.triotech.hrms.R;
import com.triotech.hrms.databinding.ItemCalendarDayBinding;

/** Feeds the Attendance Calendar's 7-column day grid, one status dot per past day. */
public class CalendarDayAdapter extends ListAdapter<CalendarDay, CalendarDayAdapter.ViewHolder> {

    public CalendarDayAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarDayBinding binding =
                ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCalendarDayBinding binding;

        ViewHolder(@NonNull ItemCalendarDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull CalendarDay day) {
            if (day.isBlank) {
                binding.textCalendarDay.setText("");
                binding.textCalendarDay.setBackground(null);
                binding.dotCalendarStatus.setVisibility(View.GONE);
                return;
            }

            binding.textCalendarDay.setText(String.valueOf(day.dayOfMonth));

            if (day.isToday) {
                binding.textCalendarDay.setBackgroundResource(R.drawable.shape_calendar_today_bg);
                binding.textCalendarDay.setTextColor(
                        MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorOnPrimaryContainer));
            } else {
                binding.textCalendarDay.setBackground(null);
                int attr = day.isWeekend
                        ? com.google.android.material.R.attr.colorOnSurfaceVariant
                        : com.google.android.material.R.attr.colorOnSurface;
                binding.textCalendarDay.setTextColor(MaterialColors.getColor(itemView, attr));
            }

            if (day.status != null) {
                binding.dotCalendarStatus.setVisibility(View.VISIBLE);
                int color = ContextCompat.getColor(itemView.getContext(), AttendanceStatusPresenter.colorRes(day.status));
                binding.dotCalendarStatus.setBackgroundTintList(ColorStateList.valueOf(color));
            } else {
                binding.dotCalendarStatus.setVisibility(View.GONE);
            }
        }
    }

    private static final DiffUtil.ItemCallback<CalendarDay> DIFF_CALLBACK = new DiffUtil.ItemCallback<CalendarDay>() {
        @Override
        public boolean areItemsTheSame(@NonNull CalendarDay oldItem, @NonNull CalendarDay newItem) {
            return oldItem.dateMillis == newItem.dateMillis && oldItem.isBlank == newItem.isBlank;
        }

        @Override
        public boolean areContentsTheSame(@NonNull CalendarDay oldItem, @NonNull CalendarDay newItem) {
            return oldItem.dayOfMonth == newItem.dayOfMonth
                    && oldItem.isToday == newItem.isToday
                    && oldItem.status == newItem.status;
        }
    };
}
