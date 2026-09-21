package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;

/** A single row in the Home dashboard's "Upcoming holidays" section. */
public class Holiday {

    private final String id;
    private final String dayNumber;
    private final String monthAbbreviation;
    private final String name;
    private final String dayOfWeekLabel;

    public Holiday(
            @NonNull String id,
            @NonNull String dayNumber,
            @NonNull String monthAbbreviation,
            @NonNull String name,
            @NonNull String dayOfWeekLabel) {
        this.id = id;
        this.dayNumber = dayNumber;
        this.monthAbbreviation = monthAbbreviation;
        this.name = name;
        this.dayOfWeekLabel = dayOfWeekLabel;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getDayNumber() {
        return dayNumber;
    }

    @NonNull
    public String getMonthAbbreviation() {
        return monthAbbreviation;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getDayOfWeekLabel() {
        return dayOfWeekLabel;
    }
}
