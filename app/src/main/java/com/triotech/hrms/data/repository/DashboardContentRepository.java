package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Announcement;
import com.triotech.hrms.data.model.DashboardSummary;
import com.triotech.hrms.data.model.Holiday;
import com.triotech.hrms.data.model.NotificationItem;
import java.util.List;

/**
 * Content for the Home dashboard's mini info cards and the three list
 * sections below the Attendance card. {@link FakeDashboardContentRepository}
 * returns static mock data for Phase 2 — this is display-only, not the real
 * leave/payroll/task/meeting modules those phases will build.
 */
public interface DashboardContentRepository {

    @NonNull
    LiveData<Resource<DashboardSummary>> getSummary();

    @NonNull
    LiveData<Resource<List<Announcement>>> getAnnouncements();

    @NonNull
    LiveData<Resource<List<Holiday>>> getUpcomingHolidays();

    @NonNull
    LiveData<Resource<List<NotificationItem>>> getRecentNotifications();
}
