package com.triotech.hrms.core.util;

import androidx.annotation.Nullable;

/**
 * Wraps a value that represents a one-time UI event (a Snackbar, a dialog, a
 * navigation trigger). Plain LiveData re-delivers its last value to every new
 * observer (e.g. on configuration change), which is wrong for events — this
 * wrapper guarantees the content is handled at most once via {@link #consume()}.
 *
 * @param <T> the event payload type.
 */
public class Event<T> {

    private final T content;
    private boolean hasBeenHandled = false;

    public Event(T content) {
        this.content = content;
    }

    /** Returns the content and marks it as handled, or null if already handled. */
    @Nullable
    public T consume() {
        if (hasBeenHandled) {
            return null;
        }
        hasBeenHandled = true;
        return content;
    }

    /** Returns the content even if it has already been handled (e.g. for logging). */
    public T peek() {
        return content;
    }
}
