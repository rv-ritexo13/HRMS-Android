package com.triotech.hrms.core.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import androidx.annotation.NonNull;
import com.triotech.hrms.data.model.Document;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Materialises a real placeholder file for a mock {@link Document} so the viewer,
 * download and share flows have something concrete to open — a company-styled
 * one-page PDF for PDF documents, or a rendered page image for JPG/PNG documents.
 * No files are bundled; everything is generated from the document metadata.
 */
public final class SampleDocumentGenerator {

    private static final String COMPANY_NAME = "Riteox Software Pvt. Ltd.";
    private static final int PAGE_WIDTH = 595;   // A4 points
    private static final int PAGE_HEIGHT = 842;
    private static final int IMG_WIDTH = 1240;   // ~150 dpi A4
    private static final int IMG_HEIGHT = 1754;

    private static final int COLOR_PRIMARY = Color.parseColor("#1A54D6");
    private static final int COLOR_TEXT = Color.parseColor("#1B1B1F");
    private static final int COLOR_MUTED = Color.parseColor("#5A5C63");
    private static final int COLOR_DIVIDER = Color.parseColor("#D9DBE1");

    private static final String[] BODY_LINES = {
            "This is a sample document generated for demonstration purposes.",
            "It stands in for the employee's real document in the Worknexa demo build.",
            "",
            "Document details, formatting and official content will be provided by",
            "the HR document service in a later phase. The layout below mirrors the",
            "structure a real company document would follow.",
            "",
            "For any queries regarding this document, please contact the People Ops team.",
    };

    private SampleDocumentGenerator() {
    }

    /** Writes a placeholder file for {@code document} to {@code target} and returns it. */
    @NonNull
    public static File writeFile(@NonNull Document document, @NonNull File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        if (document.getFileType() == Document.FileType.PDF) {
            writePdf(document, target);
        } else {
            writeImage(document, target);
        }
        return target;
    }

    private static void writePdf(@NonNull Document document, @NonNull File target) throws IOException {
        PdfDocument pdf = new PdfDocument();
        try {
            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
            PdfDocument.Page page = pdf.startPage(info);
            drawContent(page.getCanvas(), document, 40f, 24f, 1f);
            pdf.finishPage(page);
            try (FileOutputStream out = new FileOutputStream(target)) {
                pdf.writeTo(out);
            }
        } finally {
            pdf.close();
        }
    }

    private static void writeImage(@NonNull Document document, @NonNull File target) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            float scale = IMG_WIDTH / (float) PAGE_WIDTH;
            drawContent(canvas, document, 40f * scale, 24f * scale, scale);
            Bitmap.CompressFormat format = document.getFileType() == Document.FileType.PNG
                    ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
            try (FileOutputStream out = new FileOutputStream(target)) {
                bitmap.compress(format, 92, out);
            }
        } finally {
            bitmap.recycle();
        }
    }

    /** Shared drawing for both PDF and image output; {@code scale} maps point sizes to pixels. */
    private static void drawContent(
            @NonNull Canvas canvas, @NonNull Document document, float margin, float top, float scale) {
        float right = canvas.getWidth() - margin;
        float y = top + 24f * scale;

        Paint title = paint(COLOR_PRIMARY, 20f * scale, Typeface.DEFAULT_BOLD);
        canvas.drawText(COMPANY_NAME, margin, y, title);
        y += 16f * scale;

        Paint small = paint(COLOR_MUTED, 9f * scale, Typeface.DEFAULT);
        canvas.drawText(document.getFileType().getLabel() + " document", margin, y, small);
        y += 14f * scale;
        divider(canvas, margin, right, y);
        y += 34f * scale;

        Paint docTitle = paint(COLOR_TEXT, 16f * scale, Typeface.DEFAULT_BOLD);
        canvas.drawText(document.getName(), margin, y, docTitle);
        y += 20f * scale;
        canvas.drawText("Date: " + DateUtils.formatLongDate(document.getDateMillis()), margin, y, small);
        y += 32f * scale;

        Paint body = paint(COLOR_TEXT, 11.5f * scale, Typeface.DEFAULT);
        for (String line : BODY_LINES) {
            canvas.drawText(line, margin, y, body);
            y += 20f * scale;
        }
    }

    private static void divider(@NonNull Canvas canvas, float left, float right, float y) {
        Paint line = new Paint();
        line.setColor(COLOR_DIVIDER);
        line.setStrokeWidth(Math.max(1f, (right - left) / 595f));
        canvas.drawLine(left, y, right, y, line);
    }

    @NonNull
    private static Paint paint(int color, float size, @NonNull Typeface typeface) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(size);
        p.setTypeface(typeface);
        return p;
    }
}
