package com.triotech.hrms.ui.attendance;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.triotech.hrms.R;
import com.triotech.hrms.data.model.AttendanceStatus;

/**
 * Maps {@link AttendanceStatus} to its display label and colors — shared by the
 * History list, the Calendar and the Monthly Summary so all three always agree
 * on what "Late" or "Half Day" looks like.
 */
final class AttendanceStatusPresenter {

    private AttendanceStatusPresenter() {
    }

    @StringRes
    static int labelRes(@NonNull AttendanceStatus status) {
        switch (status) {
            case PRESENT:
                return R.string.attendance_status_present;
            case LATE:
                return R.string.attendance_status_late;
            case HALF_DAY:
                return R.string.attendance_status_half_day;
            case ABSENT:
                return R.string.attendance_status_absent;
            case LEAVE:
                return R.string.attendance_status_leave;
            case WORK_FROM_HOME:
            default:
                return R.string.attendance_status_wfh;
        }
    }

    @ColorRes
    static int colorRes(@NonNull AttendanceStatus status) {
        switch (status) {
            case PRESENT:
                return R.color.hrms_status_success;
            case LATE:
                return R.color.hrms_status_warning;
            case HALF_DAY:
                return R.color.hrms_status_half_day;
            case ABSENT:
                return R.color.hrms_status_absent;
            case LEAVE:
                return R.color.hrms_status_leave;
            case WORK_FROM_HOME:
            default:
                return R.color.hrms_status_info;
        }
    }

    @ColorRes
    static int containerColorRes(@NonNull AttendanceStatus status) {
        switch (status) {
            case PRESENT:
                return R.color.hrms_status_success_container;
            case LATE:
                return R.color.hrms_status_warning_container;
            case HALF_DAY:
                return R.color.hrms_status_half_day_container;
            case ABSENT:
                return R.color.hrms_status_absent_container;
            case LEAVE:
                return R.color.hrms_status_leave_container;
            case WORK_FROM_HOME:
            default:
                return R.color.hrms_status_info_container;
        }
    }
}
