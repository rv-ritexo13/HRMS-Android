package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;

/** A single row in the Home dashboard's "Recent notifications" section. */
public class NotificationItem {

    private final String id;
    private final String title;
    private final String message;
    private final String timeLabel;
    private final boolean read;

    public NotificationItem(
            @NonNull String id,
            @NonNull String title,
            @NonNull String message,
            @NonNull String timeLabel,
            boolean read) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timeLabel = timeLabel;
        this.read = read;
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
    public String getMessage() {
        return message;
    }

    @NonNull
    public String getTimeLabel() {
        return timeLabel;
    }

    public boolean isRead() {
        return read;
    }
}
