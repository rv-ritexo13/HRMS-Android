package com.triotech.hrms.data.repository;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Announcement;
import com.triotech.hrms.data.model.DashboardSummary;
import com.triotech.hrms.data.model.Holiday;
import com.triotech.hrms.data.model.NotificationItem;
import java.util.Arrays;
import java.util.List;

/** In-memory mock content for the Home dashboard. No backend yet. */
public class FakeDashboardContentRepository implements DashboardContentRepository {

    private static final long SIMULATED_LATENCY_MS = 450L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @NonNull
    @Override
    public LiveData<Resource<DashboardSummary>> getSummary() {
        MutableLiveData<Resource<DashboardSummary>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading());
        mainHandler.postDelayed(() -> liveData.setValue(Resource.success(
                new DashboardSummary(12, "₹85,000", 5, "Sprint Planning", "Today, 3:00 PM"))),
                SIMULATED_LATENCY_MS);
        return liveData;
    }

    @NonNull
    @Override
    public LiveData<Resource<List<Announcement>>> getAnnouncements() {
        MutableLiveData<Resource<List<Announcement>>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading());
        mainHandler.postDelayed(() -> {
            List<Announcement> items = Arrays.asList(
                    new Announcement("ANN-1", "Q3 town hall scheduled",
                            "Join the all-hands session to hear about this quarter's goals and wins.", "2 days ago"),
                    new Announcement("ANN-2", "New wellness benefits rolled out",
                            "Expanded health coverage and a wellness stipend are now active for all employees.",
                            "5 days ago"),
                    new Announcement("ANN-3", "Office WiFi maintenance this weekend",
                            "Expect brief connectivity drops on Saturday between 10 PM and midnight.", "1 week ago"));
            liveData.setValue(items.isEmpty() ? Resource.empty() : Resource.success(items));
        }, SIMULATED_LATENCY_MS);
        return liveData;
    }

    @NonNull
    @Override
    public LiveData<Resource<List<Holiday>>> getUpcomingHolidays() {
        MutableLiveData<Resource<List<Holiday>>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading());
        mainHandler.postDelayed(() -> {
            // Mandatory / gazetted holidays for 2026 (India). Weekdays verified
            // against the 2026 calendar (1 Jan 2026 = Thursday).
            List<Holiday> items = Arrays.asList(
                    new Holiday("HOL-1", "01", "JAN", "New Year's Day", "Thursday"),
                    new Holiday("HOL-2", "26", "JAN", "Republic Day", "Monday"),
                    new Holiday("HOL-3", "04", "MAR", "Holi", "Wednesday"),
                    new Holiday("HOL-4", "03", "APR", "Good Friday", "Friday"),
                    new Holiday("HOL-5", "14", "APR", "Ambedkar Jayanti", "Tuesday"),
                    new Holiday("HOL-6", "01", "MAY", "May Day", "Friday"),
                    new Holiday("HOL-7", "15", "AUG", "Independence Day", "Saturday"),
                    new Holiday("HOL-8", "02", "OCT", "Gandhi Jayanti", "Friday"),
                    new Holiday("HOL-9", "20", "OCT", "Dussehra", "Tuesday"),
                    new Holiday("HOL-10", "08", "NOV", "Diwali", "Sunday"),
                    new Holiday("HOL-11", "25", "DEC", "Christmas", "Friday"));
            liveData.setValue(items.isEmpty() ? Resource.empty() : Resource.success(items));
        }, SIMULATED_LATENCY_MS);
        return liveData;
    }

    @NonNull
    @Override
    public LiveData<Resource<List<NotificationItem>>> getRecentNotifications() {
        MutableLiveData<Resource<List<NotificationItem>>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading());
        mainHandler.postDelayed(() -> {
            List<NotificationItem> items = Arrays.asList(
                    new NotificationItem("NOTIF-1", "Leave request approved",
                            "Your leave for Oct 2 has been approved by your manager.", "1h ago", false),
                    new NotificationItem("NOTIF-2", "Payslip generated",
                            "Your September payslip is ready to view.", "1 day ago", true),
                    new NotificationItem("NOTIF-3", "Policy update",
                            "The work-from-home policy has been updated. Please review it.", "3 days ago", true));
            liveData.setValue(items.isEmpty() ? Resource.empty() : Resource.success(items));
        }, SIMULATED_LATENCY_MS);
        return liveData;
    }
}
