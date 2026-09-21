package com.triotech.hrms.ui.dashboard;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.triotech.hrms.data.model.Holiday;
import com.triotech.hrms.databinding.ItemHolidayBinding;

/** Feeds the Home dashboard's "Upcoming holidays" section. */
public class HolidayAdapter extends ListAdapter<Holiday, HolidayAdapter.ViewHolder> {

    public HolidayAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHolidayBinding binding = ItemHolidayBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHolidayBinding binding;

        ViewHolder(@NonNull ItemHolidayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Holiday item) {
            binding.textHolidayDay.setText(item.getDayNumber());
            binding.textHolidayMonth.setText(item.getMonthAbbreviation());
            binding.textHolidayName.setText(item.getName());
            binding.textHolidayDayOfWeek.setText(item.getDayOfWeekLabel());
        }
    }

    private static final DiffUtil.ItemCallback<Holiday> DIFF_CALLBACK = new DiffUtil.ItemCallback<Holiday>() {
        @Override
        public boolean areItemsTheSame(@NonNull Holiday oldItem, @NonNull Holiday newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Holiday oldItem, @NonNull Holiday newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getDayNumber().equals(newItem.getDayNumber())
                    && oldItem.getMonthAbbreviation().equals(newItem.getMonthAbbreviation());
        }
    };
}
