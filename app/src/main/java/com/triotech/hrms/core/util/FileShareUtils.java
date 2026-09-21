package com.triotech.hrms.core.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import java.io.File;

/**
 * Thin wrapper around {@link FileProvider} for opening and sharing generated files
 * (payslips, documents) through the app's {@code ${applicationId}.fileprovider}
 * authority. Callers handle {@link android.content.ActivityNotFoundException}.
 */
public final class FileShareUtils {

    private FileShareUtils() {
    }

    @NonNull
    public static Uri uriFor(@NonNull Context context, @NonNull File file) {
        return FileProvider.getUriForFile(
                context, context.getPackageName() + ".fileprovider", file);
    }

    /** Opens the file in an external viewer app. Throws if none is installed. */
    public static void open(@NonNull Context context, @NonNull File file, @NonNull String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uriFor(context, file), mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /** Shows the system share sheet for the file. */
    public static void share(
            @NonNull Context context, @NonNull File file, @NonNull String mimeType,
            @NonNull String subject, @NonNull String chooserTitle) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uriFor(context, file));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, chooserTitle));
    }
}
