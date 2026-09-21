package com.triotech.hrms.data.repository;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Goal;
import com.triotech.hrms.data.model.Kra;
import com.triotech.hrms.data.model.PerformanceOverview;
import com.triotech.hrms.data.model.PerformanceReview;
import java.util.Arrays;

/**
 * In-memory mock for the Performance Management screen. Deterministic demo data
 * for the current appraisal cycle — no backend yet.
 */
public class MockPerformanceRepository implements PerformanceRepository {

    private static final long SIMULATED_LATENCY_MS = 500L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final PerformanceOverview overview = buildOverview();

    @NonNull
    @Override
    public LiveData<Resource<PerformanceOverview>> observePerformance() {
        MutableLiveData<Resource<PerformanceOverview>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading());
        mainHandler.postDelayed(() -> liveData.setValue(Resource.success(overview)), SIMULATED_LATENCY_MS);
        return liveData;
    }

    @NonNull
    private static PerformanceOverview buildOverview() {
        return new PerformanceOverview(
                Arrays.asList(
                        new PerformanceReview("rev-h1-2026", "H1 2026 Appraisal", "Jan 2026 - Jun 2026",
                                "Anita Deshmukh", "4.3 / 5", PerformanceReview.Status.COMPLETED),
                        new PerformanceReview("rev-h2-2026", "H2 2026 Appraisal", "Jul 2026 - Dec 2026",
                                "Anita Deshmukh", null, PerformanceReview.Status.IN_PROGRESS),
                        new PerformanceReview("rev-annual-2026", "Annual Review 2026", "Dec 2026",
                                "Rohit Nair", null, PerformanceReview.Status.UPCOMING)),
                Arrays.asList(
                        new Goal("goal-1", "Ship attendance module v2",
                                "Deliver geofenced check-in and correction workflow to production.",
                                80, "31 Oct 2026", Goal.Status.ON_TRACK),
                        new Goal("goal-2", "Reduce crash-free rate gap",
                                "Bring the app crash-free sessions rate above 99.5%.",
                                45, "30 Nov 2026", Goal.Status.AT_RISK),
                        new Goal("goal-3", "Mentor two junior engineers",
                                "Run weekly pairing and code-review sessions through the half.",
                                100, "30 Jun 2026", Goal.Status.COMPLETED)),
                Arrays.asList(
                        new Kra("kra-1", "Delivery & Quality", 40,
                                "Ship 4 modules on schedule", "3 of 4 shipped", "4.0 / 5"),
                        new Kra("kra-2", "Code Quality & Reviews", 25,
                                "Review SLA under 24h", "Avg 18h turnaround", "4.5 / 5"),
                        new Kra("kra-3", "Collaboration & Ownership", 20,
                                "Lead 2 cross-team initiatives", "2 led", "4.2 / 5"),
                        new Kra("kra-4", "Learning & Growth", 15,
                                "Complete 2 certifications", "1 completed", "3.5 / 5")));
    }
}
