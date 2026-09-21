package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;

/** A single row in the Home dashboard's "Recent announcements" section. */
public class Announcement {

    private final String id;
    private final String title;
    private final String body;
    private final String postedLabel;

    public Announcement(
            @NonNull String id, @NonNull String title, @NonNull String body, @NonNull String postedLabel) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.postedLabel = postedLabel;
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
    public String getBody() {
        return body;
    }

    @NonNull
    public String getPostedLabel() {
        return postedLabel;
    }
}
