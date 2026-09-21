package com.triotech.hrms.core.util;

import androidx.annotation.NonNull;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formats whole-rupee amounts the way an Indian payslip reads: a leading ₹ and
 * Indian digit grouping (e.g. 1,00,000 rather than 100,000), no paise. Used by
 * the Salary screens and the generated payslip PDF so on-screen and printed
 * figures always match.
 */
public final class CurrencyUtils {

    private static final Locale INDIA = new Locale("en", "IN");

    private CurrencyUtils() {
    }

    /** e.g. {@code 79000 -> "₹79,000"}. */
    @NonNull
    public static String formatRupees(long amount) {
        NumberFormat format = NumberFormat.getInstance(INDIA);
        format.setMaximumFractionDigits(0);
        return "₹" + format.format(amount);
    }
}
