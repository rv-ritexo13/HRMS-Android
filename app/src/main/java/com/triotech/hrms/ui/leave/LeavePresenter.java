package com.triotech.hrms.ui.leave;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.triotech.hrms.R;
import com.triotech.hrms.data.model.LeaveStatus;
import com.triotech.hrms.data.model.LeaveType;

/**
 * Maps {@link LeaveType} and {@link LeaveStatus} to their display label, icon and
 * colours. Keeps the data models free of Android/resource references (mirrors the
 * approach used by the Attendance module's status presenter).
 */
public final class LeavePresenter {

    private LeavePresenter() {
    }

    @StringRes
    public static int labelFor(@NonNull LeaveType type) {
        switch (type) {
            case SICK:
                return R.string.leave_type_sick;
            case EARNED:
                return R.string.leave_type_earned;
            case WORK_FROM_HOME:
                return R.string.leave_type_wfh;
            case OPTIONAL_HOLIDAY:
                return R.string.leave_type_optional;
            case CASUAL:
            default:
                return R.string.leave_type_casual;
        }
    }

    @DrawableRes
    public static int iconFor(@NonNull LeaveType type) {
        switch (type) {
            case SICK:
                return R.drawable.ic_emergency;
            case EARNED:
                return R.drawable.ic_holiday;
            case WORK_FROM_HOME:
                return R.drawable.ic_work_outline;
            case OPTIONAL_HOLIDAY:
                return R.drawable.ic_event_available;
            case CASUAL:
            default:
                return R.drawable.ic_calendar;
        }
    }

    @ColorRes
    public static int accentColorFor(@NonNull LeaveType type) {
        switch (type) {
            case SICK:
                return R.color.hrms_status_absent;
            case EARNED:
                return R.color.hrms_status_success;
            case WORK_FROM_HOME:
                return R.color.hrms_status_warning;
            case OPTIONAL_HOLIDAY:
                return R.color.hrms_status_leave;
            case CASUAL:
            default:
                return R.color.hrms_status_info;
        }
    }

    @StringRes
    public static int labelFor(@NonNull LeaveStatus status) {
        switch (status) {
            case APPROVED:
                return R.string.leave_status_approved;
            case REJECTED:
                return R.string.leave_status_rejected;
            case CANCELLED:
                return R.string.leave_status_cancelled;
            case PENDING:
            default:
                return R.string.leave_status_pending;
        }
    }

    @ColorRes
    public static int statusTextColor(@NonNull LeaveStatus status) {
        switch (status) {
            case APPROVED:
                return R.color.hrms_status_success;
            case REJECTED:
                return R.color.hrms_status_absent;
            case CANCELLED:
                return R.color.hrms_status_info;
            case PENDING:
            default:
                return R.color.hrms_status_warning;
        }
    }

    @ColorRes
    public static int statusContainerColor(@NonNull LeaveStatus status) {
        switch (status) {
            case APPROVED:
                return R.color.hrms_status_success_container;
            case REJECTED:
                return R.color.hrms_status_absent_container;
            case CANCELLED:
                return R.color.hrms_status_info_container;
            case PENDING:
            default:
                return R.color.hrms_status_warning_container;
        }
    }
}
