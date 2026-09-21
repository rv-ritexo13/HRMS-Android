package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;

/** A Key Result Area in the Performance Management screen's KRA section. */
public class Kra {

    private final String id;
    private final String title;
    private final int weightagePercent;
    private final String targetLabel;
    private final String achievementLabel;
    private final String ratingLabel;

    public Kra(
            @NonNull String id,
            @NonNull String title,
            int weightagePercent,
            @NonNull String targetLabel,
            @NonNull String achievementLabel,
            @NonNull String ratingLabel) {
        this.id = id;
        this.title = title;
        this.weightagePercent = weightagePercent;
        this.targetLabel = targetLabel;
        this.achievementLabel = achievementLabel;
        this.ratingLabel = ratingLabel;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public int getWeightagePercent() {
        return weightagePercent;
    }

    @NonNull
    public String getTargetLabel() {
        return targetLabel;
    }

    @NonNull
    public String getAchievementLabel() {
        return achievementLabel;
    }

    @NonNull
    public String getRatingLabel() {
        return ratingLabel;
    }
}
