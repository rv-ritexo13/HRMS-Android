package com.triotech.hrms.ui.expenses;

import android.content.Context;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.triotech.hrms.R;
import com.triotech.hrms.data.model.ExpenseCategory;
import com.triotech.hrms.data.model.ExpenseReport;
import com.triotech.hrms.data.model.ExpenseStatus;
import java.util.Map;

/**
 * Maps {@link ExpenseCategory} and {@link ExpenseStatus} to their display label,
 * icon and colours. Keeps the data models free of Android/resource references
 * (mirrors {@code LeavePresenter}).
 */
public final class ExpensePresenter {

    private ExpensePresenter() {
    }

    @StringRes
    public static int labelFor(@NonNull ExpenseCategory category) {
        switch (category) {
            case TRAVEL:
                return R.string.expense_category_travel;
            case FOOD:
                return R.string.expense_category_food;
            case ACCOMMODATION:
                return R.string.expense_category_accommodation;
            case TRANSPORTATION:
                return R.string.expense_category_transportation;
            case OFFICE:
                return R.string.expense_category_office;
            case MEDICAL:
                return R.string.expense_category_medical;
            case OTHER:
            default:
                return R.string.expense_category_other;
        }
    }

    @DrawableRes
    public static int iconFor(@NonNull ExpenseCategory category) {
        switch (category) {
            case TRAVEL:
                return R.drawable.ic_location;
            case FOOD:
                return R.drawable.ic_holiday;
            case ACCOMMODATION:
                return R.drawable.ic_home;
            case TRANSPORTATION:
                return R.drawable.ic_attendance;
            case OFFICE:
                return R.drawable.ic_work_outline;
            case MEDICAL:
                return R.drawable.ic_emergency;
            case OTHER:
            default:
                return R.drawable.ic_folder;
        }
    }

    @ColorRes
    public static int accentColorFor(@NonNull ExpenseCategory category) {
        switch (category) {
            case TRAVEL:
                return R.color.hrms_status_info;
            case FOOD:
                return R.color.hrms_status_warning;
            case ACCOMMODATION:
                return R.color.hrms_status_leave;
            case TRANSPORTATION:
                return R.color.hrms_status_half_day;
            case OFFICE:
                return R.color.hrms_status_success;
            case MEDICAL:
                return R.color.hrms_status_absent;
            case OTHER:
            default:
                return R.color.hrms_status_info;
        }
    }

    @StringRes
    public static int labelFor(@NonNull ExpenseStatus status) {
        switch (status) {
            case SUBMITTED:
                return R.string.expense_status_submitted;
            case APPROVED:
                return R.string.expense_status_approved;
            case REJECTED:
                return R.string.expense_status_rejected;
            case PAID:
                return R.string.expense_status_paid;
            case DRAFT:
            default:
                return R.string.expense_status_draft;
        }
    }

    @ColorRes
    public static int statusTextColor(@NonNull ExpenseStatus status) {
        switch (status) {
            case SUBMITTED:
                return R.color.hrms_status_warning;
            case APPROVED:
                return R.color.hrms_status_success;
            case REJECTED:
                return R.color.hrms_status_absent;
            case PAID:
                return R.color.hrms_status_half_day;
            case DRAFT:
            default:
                return R.color.hrms_status_info;
        }
    }

    /** A compact "3 Approved · 1 Draft" summary of a report's expenses by status. */
    @NonNull
    public static String reportStatusBreakdown(@NonNull Context context, @NonNull ExpenseReport report) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<ExpenseStatus, Integer> entry : report.statusCounts().entrySet()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(entry.getValue()).append(' ').append(context.getString(labelFor(entry.getKey())));
        }
        return sb.toString();
    }

    @ColorRes
    public static int statusContainerColor(@NonNull ExpenseStatus status) {
        switch (status) {
            case SUBMITTED:
                return R.color.hrms_status_warning_container;
            case APPROVED:
                return R.color.hrms_status_success_container;
            case REJECTED:
                return R.color.hrms_status_absent_container;
            case PAID:
                return R.color.hrms_status_half_day_container;
            case DRAFT:
            default:
                return R.color.hrms_status_info_container;
        }
    }
}
