package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;
import java.util.List;

/** Everything the Performance Management screen renders, in one payload. */
public class PerformanceOverview {

    private final List<PerformanceReview> reviews;
    private final List<Goal> goals;
    private final List<Kra> kras;

    public PerformanceOverview(
            @NonNull List<PerformanceReview> reviews,
            @NonNull List<Goal> goals,
            @NonNull List<Kra> kras) {
        this.reviews = reviews;
        this.goals = goals;
        this.kras = kras;
    }

    @NonNull
    public List<PerformanceReview> getReviews() {
        return reviews;
    }

    @NonNull
    public List<Goal> getGoals() {
        return goals;
    }

    @NonNull
    public List<Kra> getKras() {
        return kras;
    }
}
