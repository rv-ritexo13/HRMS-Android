package com.triotech.hrms.ui.dashboard;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import java.util.Locale;

/** One searchable app feature: what it's called, how to find it, and where it opens. */
class FeatureSearchTarget {

    @IdRes private final int destinationId;
    @DrawableRes private final int iconRes;
    @StringRes private final int titleRes;
    @StringRes private final int subtitleRes;
    /** Lowercase label + synonyms, matched as a substring against the query. */
    private final String haystack;

    FeatureSearchTarget(
            @IdRes int destinationId,
            @DrawableRes int iconRes,
            @StringRes int titleRes,
            @StringRes int subtitleRes,
            @NonNull String keywords) {
        this.destinationId = destinationId;
        this.iconRes = iconRes;
        this.titleRes = titleRes;
        this.subtitleRes = subtitleRes;
        this.haystack = keywords.toLowerCase(Locale.getDefault());
    }

    @IdRes
    int getDestinationId() {
        return destinationId;
    }

    @DrawableRes
    int getIconRes() {
        return iconRes;
    }

    @StringRes
    int getTitleRes() {
        return titleRes;
    }

    @StringRes
    int getSubtitleRes() {
        return subtitleRes;
    }

    boolean matches(@NonNull String query) {
        return haystack.contains(query);
    }
}
