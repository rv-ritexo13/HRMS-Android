package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;
import java.util.Locale;

/**
 * A single document in the employee's Documents screen (mock catalog for now).
 * The actual file is materialised on demand by
 * {@link com.triotech.hrms.core.util.SampleDocumentGenerator} when the user views,
 * downloads or shares it — no real files are bundled or stored yet.
 */
public final class Document {

    /** Grouping shown as section headers on the Documents screen. */
    public enum Category {
        PAYSLIPS,
        OFFER_LETTER,
        APPOINTMENT_LETTER,
        EXPERIENCE_LETTER,
        TAX_DOCUMENTS,
        COMPANY_POLICIES,
        OTHER
    }

    public enum FileType {
        PDF("PDF", "application/pdf", "pdf"),
        JPG("JPG", "image/jpeg", "jpg"),
        PNG("PNG", "image/png", "png");

        private final String label;
        private final String mimeType;
        private final String extension;

        FileType(String label, String mimeType, String extension) {
            this.label = label;
            this.mimeType = mimeType;
            this.extension = extension;
        }

        @NonNull
        public String getLabel() {
            return label;
        }

        @NonNull
        public String getMimeType() {
            return mimeType;
        }

        @NonNull
        public String getExtension() {
            return extension;
        }

        public boolean isImage() {
            return this == JPG || this == PNG;
        }
    }

    private final String id;
    private final Category category;
    private final String name;
    private final FileType fileType;
    private final long dateMillis;
    private final long sizeBytes;

    public Document(
            @NonNull String id,
            @NonNull Category category,
            @NonNull String name,
            @NonNull FileType fileType,
            long dateMillis,
            long sizeBytes) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.fileType = fileType;
        this.dateMillis = dateMillis;
        this.sizeBytes = sizeBytes;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public Category getCategory() {
        return category;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public FileType getFileType() {
        return fileType;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    /** A human-readable size, e.g. "248 KB" or "1.2 MB". */
    @NonNull
    public String getSizeLabel() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        double kb = sizeBytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.getDefault(), "%.0f KB", kb);
        }
        return String.format(Locale.getDefault(), "%.1f MB", kb / 1024.0);
    }

    /** A filesystem-safe base file name (without directory), including extension. */
    @NonNull
    public String getFileName() {
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe + "." + fileType.getExtension();
    }
}
