package com.triotech.hrms.ui.employees;

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
import com.triotech.hrms.data.model.Employee;
import com.triotech.hrms.databinding.ItemEmployeeBinding;

/**
 * RecyclerView adapter for the employee directory. Uses {@link ListAdapter} so
 * updates (e.g. after a pull-to-refresh) are diffed automatically instead of a
 * blanket {@code notifyDataSetChanged()}.
 */
public class EmployeeAdapter extends ListAdapter<Employee, EmployeeAdapter.EmployeeViewHolder> {

    /** Optional row-click callback; null (the default) keeps rows inert for Phase 1. */
    @Nullable private OnEmployeeClickListener listener;

    public interface OnEmployeeClickListener {
        void onEmployeeClick(@NonNull Employee employee);
    }

    public EmployeeAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnEmployeeClickListener(@Nullable OnEmployeeClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEmployeeBinding binding = ItemEmployeeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new EmployeeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {

        private final ItemEmployeeBinding binding;

        EmployeeViewHolder(@NonNull ItemEmployeeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Employee employee, @Nullable OnEmployeeClickListener listener) {
            binding.textAvatarInitials.setText(employee.getInitials());
            binding.textEmployeeName.setText(employee.getFullName());
            binding.textEmployeeDesignation.setText(
                    employee.getDesignation() + " · " + employee.getDepartment());

            applyStatus(employee.getStatus());

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEmployeeClick(employee);
                }
            });
        }

        private void applyStatus(@NonNull Employee.EmploymentStatus status) {
            int textColorRes;
            int bgColorRes;
            String label;
            switch (status) {
                case ON_LEAVE:
                    label = "On Leave";
                    textColorRes = R.color.hrms_status_warning;
                    bgColorRes = R.color.hrms_status_warning_container;
                    break;
                case NOTICE_PERIOD:
                    label = "Notice Period";
                    textColorRes = R.color.hrms_status_info;
                    bgColorRes = R.color.hrms_status_info_container;
                    break;
                case ACTIVE:
                default:
                    label = "Active";
                    textColorRes = R.color.hrms_status_success;
                    bgColorRes = R.color.hrms_status_success_container;
                    break;
            }
            binding.chipStatus.setText(label);
            binding.chipStatus.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), textColorRes));
            binding.chipStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(binding.getRoot().getContext(), bgColorRes)));
        }
    }

    private static final DiffUtil.ItemCallback<Employee> DIFF_CALLBACK = new DiffUtil.ItemCallback<Employee>() {
        @Override
        public boolean areItemsTheSame(@NonNull Employee oldItem, @NonNull Employee newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Employee oldItem, @NonNull Employee newItem) {
            return oldItem.getFullName().equals(newItem.getFullName())
                    && oldItem.getDesignation().equals(newItem.getDesignation())
                    && oldItem.getDepartment().equals(newItem.getDepartment())
                    && oldItem.getStatus() == newItem.getStatus();
        }
    };
}
