package com.triotech.hrms.core.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import androidx.annotation.NonNull;
import com.triotech.hrms.data.model.Payslip;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Renders a {@link Payslip} into a professional, company-style PDF using the
 * framework {@link PdfDocument} (no third-party PDF library). Layout is a single
 * A4 page: company header, employee details, an earnings table, a deductions
 * table, and a highlighted net-pay total.
 *
 * <p>All coordinates are in PostScript points (72 pt = 1 inch); an A4 page is
 * 595 × 842 pt.</p>
 */
public final class PayslipPdfGenerator {

    private static final String COMPANY_NAME = "Riteox Software Pvt. Ltd.";
    private static final String COMPANY_ADDRESS = "4th Floor, Prestige Tech Park, Bengaluru 560103";

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final float MARGIN = 40f;
    private static final float CONTENT_RIGHT = PAGE_WIDTH - MARGIN;

    private static final int COLOR_PRIMARY = Color.parseColor("#1A54D6");
    private static final int COLOR_TEXT = Color.parseColor("#1B1B1F");
    private static final int COLOR_MUTED = Color.parseColor("#5A5C63");
    private static final int COLOR_DIVIDER = Color.parseColor("#D9DBE1");
    private static final int COLOR_NET_BG = Color.parseColor("#E7EEFF");

    private PayslipPdfGenerator() {
    }

    /** Writes a payslip PDF for {@code payslip} to {@code target} and returns it. */
    @NonNull
    public static File writePdf(@NonNull Payslip payslip, @NonNull File target) throws IOException {
        PdfDocument document = new PdfDocument();
        try {
            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            drawPage(page.getCanvas(), payslip);
            document.finishPage(page);

            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            try (FileOutputStream out = new FileOutputStream(target)) {
                document.writeTo(out);
            }
            return target;
        } finally {
            document.close();
        }
    }

    private static void drawPage(@NonNull Canvas canvas, @NonNull Payslip payslip) {
        Paint title = paint(COLOR_PRIMARY, 20f, Typeface.DEFAULT_BOLD);
        Paint heading = paint(COLOR_TEXT, 13f, Typeface.DEFAULT_BOLD);
        Paint label = paint(COLOR_MUTED, 10.5f, Typeface.DEFAULT);
        Paint value = paint(COLOR_TEXT, 11.5f, Typeface.DEFAULT_BOLD);
        Paint rowLabel = paint(COLOR_TEXT, 11.5f, Typeface.DEFAULT);
        Paint rowAmount = paint(COLOR_TEXT, 11.5f, Typeface.DEFAULT_BOLD);
        rowAmount.setTextAlign(Paint.Align.RIGHT);
        Paint small = paint(COLOR_MUTED, 9f, Typeface.DEFAULT);

        float y = MARGIN + 8f;

        // ---- Header ----
        canvas.drawText(COMPANY_NAME, MARGIN, y, title);
        Paint titleRight = paint(COLOR_MUTED, 12f, Typeface.DEFAULT_BOLD);
        titleRight.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("PAYSLIP", CONTENT_RIGHT, y, titleRight);
        y += 16f;
        canvas.drawText(COMPANY_ADDRESS, MARGIN, y, small);
        Paint periodRight = paint(COLOR_TEXT, 11f, Typeface.DEFAULT);
        periodRight.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(
                "Pay Period: " + DateUtils.formatMonthYear(payslip.getYear(), payslip.getMonth()),
                CONTENT_RIGHT, y, periodRight);
        y += 18f;
        divider(canvas, y);
        y += 24f;

        // ---- Employee details (two columns of key/value) ----
        float colGap = (CONTENT_RIGHT - MARGIN) / 2f;
        float leftX = MARGIN;
        float rightX = MARGIN + colGap;
        float startY = y;

        y = keyValue(canvas, leftX, y, "Employee Name", payslip.getEmployeeName(), label, value);
        y = keyValue(canvas, leftX, y, "Employee ID", payslip.getEmployeeId(), label, value);
        y = keyValue(canvas, leftX, y, "Department", payslip.getDepartment(), label, value);

        float yRight = startY;
        yRight = keyValue(canvas, rightX, yRight, "Designation", payslip.getDesignation(), label, value);
        yRight = keyValue(canvas, rightX, yRight, "Pay Date",
                DateUtils.formatLongDate(payslip.getCreditDateMillis()), label, value);
        yRight = keyValue(canvas, rightX, yRight, "Status",
                payslip.isCredited() ? "Credited" : "Pending", label, value);

        y = Math.max(y, yRight) + 10f;
        divider(canvas, y);
        y += 26f;

        // ---- Earnings ----
        canvas.drawText("Earnings", MARGIN, y, heading);
        y += 18f;
        y = amountRow(canvas, y, "Basic Salary", payslip.getBasic(), rowLabel, rowAmount);
        y = amountRow(canvas, y, "House Rent Allowance (HRA)", payslip.getHra(), rowLabel, rowAmount);
        y = amountRow(canvas, y, "Special Allowance", payslip.getSpecialAllowance(), rowLabel, rowAmount);
        y = amountRow(canvas, y, "Other Allowances", payslip.getOtherAllowances(), rowLabel, rowAmount);
        y += 4f;
        divider(canvas, y);
        y += 18f;
        y = totalRow(canvas, y, "Gross Earnings", payslip.getGrossSalary());
        y += 22f;

        // ---- Deductions ----
        canvas.drawText("Deductions", MARGIN, y, heading);
        y += 18f;
        y = amountRow(canvas, y, "Provident Fund (PF)", payslip.getProvidentFund(), rowLabel, rowAmount);
        y = amountRow(canvas, y, "Professional Tax", payslip.getProfessionalTax(), rowLabel, rowAmount);
        y = amountRow(canvas, y, "Other Deductions", payslip.getOtherDeductions(), rowLabel, rowAmount);
        y += 4f;
        divider(canvas, y);
        y += 18f;
        y = totalRow(canvas, y, "Total Deductions", payslip.getTotalDeductions());
        y += 26f;

        // ---- Net salary box ----
        Paint boxFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxFill.setColor(COLOR_NET_BG);
        float boxTop = y;
        float boxBottom = y + 46f;
        canvas.drawRoundRect(MARGIN, boxTop, CONTENT_RIGHT, boxBottom, 8f, 8f, boxFill);

        Paint netLabel = paint(COLOR_PRIMARY, 13f, Typeface.DEFAULT_BOLD);
        Paint netValue = paint(COLOR_PRIMARY, 18f, Typeface.DEFAULT_BOLD);
        netValue.setTextAlign(Paint.Align.RIGHT);
        float netBaseline = boxTop + 30f;
        canvas.drawText("Net Salary", MARGIN + 16f, netBaseline, netLabel);
        canvas.drawText(CurrencyUtils.formatRupees(payslip.getNetSalary()),
                CONTENT_RIGHT - 16f, netBaseline, netValue);
        y = boxBottom + 30f;

        // ---- Footer ----
        canvas.drawText(
                "This is a computer-generated payslip and does not require a signature.",
                MARGIN, y, small);
    }

    private static float keyValue(
            @NonNull Canvas canvas, float x, float y,
            @NonNull String key, @NonNull String val,
            @NonNull Paint label, @NonNull Paint value) {
        canvas.drawText(key, x, y, label);
        canvas.drawText(val, x, y + 15f, value);
        return y + 36f;
    }

    private static float amountRow(
            @NonNull Canvas canvas, float y,
            @NonNull String label, long amount,
            @NonNull Paint labelPaint, @NonNull Paint amountPaint) {
        canvas.drawText(label, MARGIN, y, labelPaint);
        canvas.drawText(CurrencyUtils.formatRupees(amount), CONTENT_RIGHT, y, amountPaint);
        return y + 20f;
    }

    private static float totalRow(@NonNull Canvas canvas, float y, @NonNull String label, long amount) {
        Paint labelPaint = paint(COLOR_TEXT, 12f, Typeface.DEFAULT_BOLD);
        Paint amountPaint = paint(COLOR_TEXT, 12f, Typeface.DEFAULT_BOLD);
        amountPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(label, MARGIN, y, labelPaint);
        canvas.drawText(CurrencyUtils.formatRupees(amount), CONTENT_RIGHT, y, amountPaint);
        return y + 20f;
    }

    private static void divider(@NonNull Canvas canvas, float y) {
        Paint line = new Paint();
        line.setColor(COLOR_DIVIDER);
        line.setStrokeWidth(1f);
        canvas.drawLine(MARGIN, y, CONTENT_RIGHT, y, line);
    }

    @NonNull
    private static Paint paint(int color, float sizePt, @NonNull Typeface typeface) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setTextSize(sizePt);
        paint.setTypeface(typeface);
        return paint;
    }
}
