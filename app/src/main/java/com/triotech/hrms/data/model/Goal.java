package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;

/** A single goal/objective in the Performance Management screen's Goals section. */
public class Goal {

    /** Progress health of the goal. */
    public enum Status {
        ON_TRACK,
        AT_RISK,
        COMPLETED
    }

    private final String id;
    private final String title;
    private final String description;
    private final int progressPercent;
    private final String dueLabel;
    private final Status status;

    public Goal(
            @NonNull String id,
            @NonNull String title,
            @NonNull String description,
            int progressPercent,
            @NonNull String dueLabel,
            @NonNull Status status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.progressPercent = progressPercent;
        this.dueLabel = dueLabel;
        this.status = status;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    @NonNull
    public String getDueLabel() {
        return dueLabel;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }
}
