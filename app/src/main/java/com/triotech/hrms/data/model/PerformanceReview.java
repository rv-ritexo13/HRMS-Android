package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** One appraisal cycle in the Performance Management screen's Reviews section. */
public class PerformanceReview {

    /** Where the cycle stands right now. */
    public enum Status {
        COMPLETED,
        IN_PROGRESS,
        UPCOMING
    }

    private final String id;
    private final String cycleName;
    private final String periodLabel;
    private final String reviewerName;
    @Nullable private final String ratingLabel;
    private final Status status;

    public PerformanceReview(
            @NonNull String id,
            @NonNull String cycleName,
            @NonNull String periodLabel,
            @NonNull String reviewerName,
            @Nullable String ratingLabel,
            @NonNull Status status) {
        this.id = id;
        this.cycleName = cycleName;
        this.periodLabel = periodLabel;
        this.reviewerName = reviewerName;
        this.ratingLabel = ratingLabel;
        this.status = status;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getCycleName() {
        return cycleName;
    }

    @NonNull
    public String getPeriodLabel() {
        return periodLabel;
    }

    @NonNull
    public String getReviewerName() {
        return reviewerName;
    }

    @Nullable
    public String getRatingLabel() {
        return ratingLabel;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }
}
