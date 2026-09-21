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
import com.triotech.hrms.data.model.AttendanceCorrectionRequest;
import com.triotech.hrms.data.model.CorrectionStatus;
import com.triotech.hrms.databinding.ItemCorrectionRequestBinding;

/** Feeds the Attendance screen's list of submitted correction requests. */
public class CorrectionAdapter extends ListAdapter<AttendanceCorrectionRequest, CorrectionAdapter.ViewHolder> {

    public CorrectionAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCorrectionRequestBinding binding =
                ItemCorrectionRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCorrectionRequestBinding binding;

        ViewHolder(@NonNull ItemCorrectionRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull AttendanceCorrectionRequest request) {
            android.content.Context context = itemView.getContext();

            binding.textCorrectionDate.setText(DateUtils.formatLongDate(request.getDateMillis()));
            binding.textCorrectionReason.setText(request.getReason());

            String actual = request.getActualCheckInMillis() != null
                    ? DateUtils.formatTime(request.getActualCheckInMillis())
                    : context.getString(R.string.attendance_correction_no_checkin);
            String expected = request.getExpectedCheckInMillis() != null
                    ? DateUtils.formatTime(request.getExpectedCheckInMillis())
                    : context.getString(R.string.attendance_value_placeholder);
            binding.textCorrectionMeta.setText(context.getString(
                    R.string.attendance_correction_meta_format, actual, expected));

            int labelRes;
            int colorRes;
            int containerRes;
            switch (request.getStatus()) {
                case APPROVED:
                    labelRes = R.string.attendance_correction_status_approved;
                    colorRes = R.color.hrms_status_success;
                    containerRes = R.color.hrms_status_success_container;
                    break;
                case REJECTED:
                    labelRes = R.string.attendance_correction_status_rejected;
                    colorRes = R.color.hrms_status_absent;
                    containerRes = R.color.hrms_status_absent_container;
                    break;
                case PENDING:
                default:
                    labelRes = R.string.attendance_correction_status_pending;
                    colorRes = R.color.hrms_status_warning;
                    containerRes = R.color.hrms_status_warning_container;
                    break;
            }
            binding.chipCorrectionStatus.setText(labelRes);
            binding.chipCorrectionStatus.setTextColor(ContextCompat.getColor(context, colorRes));
            binding.chipCorrectionStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(context, containerRes)));
        }
    }

    private static final DiffUtil.ItemCallback<AttendanceCorrectionRequest> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AttendanceCorrectionRequest>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AttendanceCorrectionRequest oldItem, @NonNull AttendanceCorrectionRequest newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AttendanceCorrectionRequest oldItem, @NonNull AttendanceCorrectionRequest newItem) {
                    return oldItem.getStatus() == newItem.getStatus()
                            && oldItem.getReason().equals(newItem.getReason());
                }
            };
}
