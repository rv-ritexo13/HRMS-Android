package com.triotech.hrms.ui.dashboard;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Announcement;
import com.triotech.hrms.data.model.AttendanceRecord;
import com.triotech.hrms.data.model.DashboardSummary;
import com.triotech.hrms.data.model.Holiday;
import com.triotech.hrms.data.model.NotificationItem;
import com.triotech.hrms.data.repository.AttendanceRepository;
import com.triotech.hrms.data.repository.DashboardContentRepository;
import java.util.List;

/**
 * Backs the Home tab (the Employee Dashboard). Combines today's attendance
 * state with the dashboard's mock content (summary, announcements, holidays,
 * notifications) — each exposed as its own stream so {@link DashboardFragment}
 * can render each card/section independently as it loads.
 */
public class DashboardViewModel extends BaseViewModel {

    private final AttendanceRepository attendanceRepository;
    private final LiveData<AttendanceRecord> attendance;
    private final LiveData<Resource<DashboardSummary>> summary;
    private final LiveData<Resource<List<Announcement>>> announcements;
    private final LiveData<Resource<List<Holiday>>> holidays;
    private final LiveData<Resource<List<NotificationItem>>> notifications;

    public DashboardViewModel(
            @NonNull AttendanceRepository attendanceRepository,
            @NonNull DashboardContentRepository contentRepository) {
        this.attendanceRepository = attendanceRepository;
        this.attendance = attendanceRepository.observeToday();
        this.summary = contentRepository.getSummary();
        this.announcements = contentRepository.getAnnouncements();
        this.holidays = contentRepository.getUpcomingHolidays();
        this.notifications = contentRepository.getRecentNotifications();
    }

    @NonNull
    public LiveData<AttendanceRecord> getAttendance() {
        return attendance;
    }

    @NonNull
    public LiveData<Resource<DashboardSummary>> getSummary() {
        return summary;
    }

    @NonNull
    public LiveData<Resource<List<Announcement>>> getAnnouncements() {
        return announcements;
    }

    @NonNull
    public LiveData<Resource<List<Holiday>>> getHolidays() {
        return holidays;
    }

    @NonNull
    public LiveData<Resource<List<NotificationItem>>> getNotifications() {
        return notifications;
    }

    /** Fresh, one-shot LiveData for the check-in action's loading/success/error state. */
    @NonNull
    public LiveData<Resource<AttendanceRecord>> checkIn() {
        return attendanceRepository.checkIn();
    }

    /** Fresh, one-shot LiveData for the check-out action's loading/success/error state. */
    @NonNull
    public LiveData<Resource<AttendanceRecord>> checkOut() {
        return attendanceRepository.checkOut();
    }
}
