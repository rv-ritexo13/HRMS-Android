package com.triotech.hrms.core.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Generic wrapper around an asynchronous operation's result, used by ViewModels
 * to expose {@code LiveData<Resource<T>>} to Fragments so a single observer can
 * drive Loading / Error / Empty / Success UI consistently across every screen.
 *
 * @param <T> the payload type on success.
 */
public final class Resource<T> {

    public enum Status { LOADING, SUCCESS, EMPTY, ERROR }

    @NonNull public final Status status;
    @Nullable public final T data;
    @Nullable public final String message;

    private Resource(@NonNull Status status, @Nullable T data, @Nullable String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null, null);
    }

    public static <T> Resource<T> success(@NonNull T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    public static <T> Resource<T> empty() {
        return new Resource<>(Status.EMPTY, null, null);
    }

    public static <T> Resource<T> error(@NonNull String message) {
        return new Resource<>(Status.ERROR, null, message);
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isEmpty() {
        return status == Status.EMPTY;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }
}
