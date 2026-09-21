package com.triotech.hrms.ui.dashboard;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.triotech.hrms.R;
import com.triotech.hrms.core.base.BaseFragment;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.core.util.SessionManager;
import com.triotech.hrms.data.model.AttendanceRecord;
import com.triotech.hrms.data.model.AuthUser;
import com.triotech.hrms.data.model.DashboardSummary;
import com.triotech.hrms.data.model.NotificationItem;
import com.triotech.hrms.data.repository.AttendanceRepository;
import com.triotech.hrms.data.repository.DashboardContentRepository;
import com.triotech.hrms.databinding.FragmentDashboardBinding;
import com.triotech.hrms.databinding.ItemStatCardBinding;
import com.triotech.hrms.databinding.LayoutDashboardSectionBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Home tab — the Employee Dashboard. Header + live attendance card (check-in/
 * check-out against the mock {@link AttendanceRepository}) + four mini info
 * cards + three list sections, all built from mock data via
 * {@link DashboardContentRepository}. GPS, payroll, KRA and leave-management
 * logic are intentionally not implemented here — those are later phases; this
 * screen only displays their headline numbers.
 */
public class DashboardFragment extends BaseFragment<FragmentDashboardBinding> {

    private DashboardViewModel viewModel;
    private final AnnouncementAdapter announcementAdapter = new AnnouncementAdapter();
    private final HolidayAdapter holidayAdapter = new HolidayAdapter();
    private final NotificationAdapter notificationAdapter = new NotificationAdapter();

    @Override
    protected FragmentDashboardBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentDashboardBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        AttendanceRepository attendanceRepository = ServiceLocator.getInstance().getAttendanceRepository();
        DashboardContentRepository contentRepository = ServiceLocator.getInstance().getDashboardContentRepository();
        viewModel = new ViewModelProvider(
                        this, new ViewModelFactory(() -> new DashboardViewModel(attendanceRepository, contentRepository)))
                .get(DashboardViewModel.class);

        bindHeader();
        setupSections();
        setupMiniCards();

        getBinding().buttonCheckIn.setOnClickListener(v -> performCheckIn());
        getBinding().buttonCheckOut.setOnClickListener(v -> performCheckOut());
        getBinding().buttonNotifications.setOnClickListener(v -> showSnackbar("Coming in a later phase"));

        viewModel.getAttendance().observe(getViewLifecycleOwner(), this::renderAttendance);
        viewModel.getSummary().observe(getViewLifecycleOwner(), this::renderSummary);
        viewModel.getAnnouncements().observe(getViewLifecycleOwner(), resource -> renderSection(
                getBinding().sectionAnnouncements, announcementAdapter, resource,
                getString(R.string.dashboard_section_empty_announcements)));
        viewModel.getHolidays().observe(getViewLifecycleOwner(), resource -> renderSection(
                getBinding().sectionHolidays, holidayAdapter, resource,
                getString(R.string.dashboard_section_empty_holidays)));
        viewModel.getNotifications().observe(getViewLifecycleOwner(), resource -> {
            renderSection(getBinding().sectionNotifications, notificationAdapter, resource,
                    getString(R.string.dashboard_section_empty_notifications));
            updateNotificationBadge(resource);
        });
    }

    private void bindHeader() {
        AuthUser user = SessionManager.getSession(requireContext());
        if (user != null) {
            getBinding().textAvatarInitials.setText(user.getInitials());
            getBinding().textUserName.setText(user.getFullName());
            getBinding().textUserRole.setText(user.getDesignation() + " · " + user.getDepartment());
        }
        String today = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(new Date());
        getBinding().textTodayDate.setText(today);
    }

    private void setupSections() {
        setupSection(getBinding().sectionAnnouncements, R.drawable.ic_announcement,
                R.string.dashboard_section_announcements, announcementAdapter);
        setupSection(getBinding().sectionHolidays, R.drawable.ic_holiday,
                R.string.dashboard_section_holidays, holidayAdapter);
        setupSection(getBinding().sectionNotifications, R.drawable.ic_notifications,
                R.string.dashboard_section_notifications, notificationAdapter);
    }

    private void setupSection(
            @NonNull LayoutDashboardSectionBinding section,
            int iconRes,
            int titleRes,
            @NonNull RecyclerView.Adapter<?> adapter) {
        section.imageSectionIcon.setImageResource(iconRes);
        section.textSectionTitle.setText(titleRes);
        section.recyclerSectionItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        section.recyclerSectionItems.setAdapter(adapter);
    }

    private void setupMiniCards() {
        setupMiniCard(getBinding().cardLeaveBalance.getRoot(), R.drawable.ic_event_available,
                R.string.dashboard_mini_leave_balance);
        setupMiniCard(getBinding().cardSalary.getRoot(), R.drawable.ic_salary, R.string.dashboard_mini_salary);
        setupMiniCard(getBinding().cardPendingTasks.getRoot(), R.drawable.ic_task,
                R.string.dashboard_mini_pending_tasks);
        setupMiniCard(getBinding().cardUpcomingMeeting.getRoot(), R.drawable.ic_meeting,
                R.string.dashboard_mini_upcoming_meeting);
    }

    private void setupMiniCard(@NonNull View cardRoot, int iconRes, int labelRes) {
        ItemStatCardBinding card = ItemStatCardBinding.bind(cardRoot);
        card.imageStatIcon.setImageResource(iconRes);
        card.textStatLabel.setText(labelRes);
        card.textStatValue.setText("—");
    }

    private void performCheckIn() {
        viewModel.checkIn().observe(getViewLifecycleOwner(), this::handleAttendanceAction);
    }

    private void performCheckOut() {
        viewModel.checkOut().observe(getViewLifecycleOwner(), this::handleAttendanceAction);
    }

    private void handleAttendanceAction(@NonNull Resource<AttendanceRecord> resource) {
        if (resource.isError() && resource.message != null) {
            showSnackbar(resource.message);
        } else if (resource.isSuccess() && resource.data != null) {
            AttendanceRecord record = resource.data;
            if (record.getStatus() == AttendanceRecord.Status.CHECKED_IN && record.getCheckInTimeMillis() != null) {
                showSnackbar(getString(R.string.dashboard_toast_checked_in_format, formatTime(record.getCheckInTimeMillis())));
            } else if (record.getCheckOutTimeMillis() != null) {
                showSnackbar(getString(R.string.dashboard_toast_checked_out_format, formatTime(record.getCheckOutTimeMillis())));
            }
        }
        // The Attendance card itself updates via viewModel.getAttendance(), which
        // observes the repository's shared "today" state — no manual re-render needed here.
    }

    private void renderAttendance(@NonNull AttendanceRecord record) {
        int statusLabelRes;
        switch (record.getStatus()) {
            case CHECKED_IN:
                statusLabelRes = R.string.dashboard_status_checked_in;
                break;
            case CHECKED_OUT:
                statusLabelRes = R.string.dashboard_status_checked_out;
                break;
            case NOT_CHECKED_IN:
            default:
                statusLabelRes = R.string.dashboard_status_not_checked_in;
                break;
        }
        getBinding().chipAttendanceStatus.setText(statusLabelRes);
        applyStatusChipColor(record.getStatus());

        getBinding().textCheckInTime.setText(record.getCheckInTimeMillis() != null
                ? formatTime(record.getCheckInTimeMillis())
                : getString(R.string.dashboard_attendance_time_placeholder));
        getBinding().textCheckOutTime.setText(record.getCheckOutTimeMillis() != null
                ? formatTime(record.getCheckOutTimeMillis())
                : getString(R.string.dashboard_attendance_time_placeholder));

        if (record.getStatus() == AttendanceRecord.Status.CHECKED_OUT) {
            getBinding().textWorkingHours.setText(formatDuration(record.getWorkingDurationMillis()));
        } else if (record.getStatus() == AttendanceRecord.Status.CHECKED_IN) {
            getBinding().textWorkingHours.setText(R.string.dashboard_attendance_hours_in_progress);
        } else {
            getBinding().textWorkingHours.setText(R.string.dashboard_attendance_hours_placeholder);
        }

        getBinding().buttonCheckIn.setEnabled(record.canCheckIn());
        getBinding().buttonCheckOut.setEnabled(record.canCheckOut());
    }

    private void applyStatusChipColor(@NonNull AttendanceRecord.Status status) {
        int textColorRes;
        int bgColorRes;
        switch (status) {
            case CHECKED_IN:
                textColorRes = R.color.hrms_status_success;
                bgColorRes = R.color.hrms_status_success_container;
                break;
            case CHECKED_OUT:
                textColorRes = R.color.hrms_status_info;
                bgColorRes = R.color.hrms_status_info_container;
                break;
            case NOT_CHECKED_IN:
            default:
                textColorRes = R.color.hrms_status_warning;
                bgColorRes = R.color.hrms_status_warning_container;
                break;
        }
        getBinding().chipAttendanceStatus.setTextColor(ContextCompat.getColor(requireContext(), textColorRes));
        getBinding().chipAttendanceStatus.setChipBackgroundColor(
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bgColorRes)));
    }

    private void renderSummary(@NonNull Resource<DashboardSummary> resource) {
        if (!resource.isSuccess() || resource.data == null) {
            return;
        }
        DashboardSummary summary = resource.data;
        getBinding().cardLeaveBalance.textStatValue.setText(
                getString(R.string.dashboard_mini_leave_balance_format, summary.getLeaveBalanceDays()));
        getBinding().cardSalary.textStatValue.setText(summary.getSalaryLabel());
        getBinding().cardPendingTasks.textStatValue.setText(String.valueOf(summary.getPendingTasksCount()));
        getBinding().cardUpcomingMeeting.textStatValue.setText(summary.getUpcomingMeetingTitle());
        getBinding().cardUpcomingMeeting.textStatLabel.setText(summary.getUpcomingMeetingTimeLabel());
    }

    private <T> void renderSection(
            @NonNull LayoutDashboardSectionBinding section,
            @NonNull ListAdapter<T, ?> adapter,
            @NonNull Resource<List<T>> resource,
            @NonNull String emptyMessage) {
        section.progressSection.setVisibility(resource.isLoading() ? View.VISIBLE : View.GONE);
        section.recyclerSectionItems.setVisibility(resource.isSuccess() ? View.VISIBLE : View.GONE);

        boolean showEmptyText = resource.isEmpty() || resource.isError();
        section.textSectionEmpty.setVisibility(showEmptyText ? View.VISIBLE : View.GONE);
        if (resource.isError()) {
            section.textSectionEmpty.setText(R.string.dashboard_section_error);
        } else if (resource.isEmpty()) {
            section.textSectionEmpty.setText(emptyMessage);
        }

        if (resource.isSuccess() && resource.data != null) {
            adapter.submitList(resource.data);
        }
    }

    private void updateNotificationBadge(@NonNull Resource<List<NotificationItem>> resource) {
        boolean hasUnread = false;
        if (resource.isSuccess() && resource.data != null) {
            for (NotificationItem item : resource.data) {
                if (!item.isRead()) {
                    hasUnread = true;
                    break;
                }
            }
        }
        getBinding().dotNotificationBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
    }

    private void showSnackbar(@NonNull String message) {
        Snackbar.make(getBinding().getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    @NonNull
    private static String formatTime(long millis) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(millis));
    }

    @NonNull
    private static String formatDuration(long millis) {
        long totalMinutes = millis / 60_000L;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours + "h " + minutes + "m";
    }
}
