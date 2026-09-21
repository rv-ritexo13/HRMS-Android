package com.triotech.hrms.core.util;

import androidx.annotation.NonNull;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Calendar-based date helpers used by the Attendance module (history filtering,
 * the monthly calendar grid, mock-data generation). The app's minSdk is 24 —
 * one release before {@code java.time} became available on-device — so all
 * date math here deliberately goes through {@link Calendar} instead.
 */
public final class DateUtils {

    private DateUtils() {
    }

    @NonNull
    public static Calendar startOfDay(@NonNull Calendar source) {
        Calendar cal = (Calendar) source.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @NonNull
    public static Calendar today() {
        return startOfDay(Calendar.getInstance());
    }

    /** Local midnight for the given year/1-12 month/day. */
    @NonNull
    public static Calendar calendarFor(int year, int month, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month - 1, dayOfMonth, 0, 0, 0);
        return cal;
    }

    @NonNull
    public static Calendar calendarFromMillis(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal;
    }

    /** A stable "yyyy-MM-dd" lookup/sort key, independent of display locale. */
    @NonNull
    public static String dateKey(@NonNull Calendar cal) {
        return dateKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    @NonNull
    public static String dateKey(int year, int month, int dayOfMonth) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, dayOfMonth);
    }

    public static boolean isSameDay(@NonNull Calendar a, @NonNull Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isWeekend(@NonNull Calendar cal) {
        int day = cal.get(Calendar.DAY_OF_WEEK);
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY;
    }

    /** True for any day strictly after today (local midnight comparison). */
    public static boolean isFuture(@NonNull Calendar cal) {
        return startOfDay(cal).after(today());
    }

    public static boolean isBeforeToday(@NonNull Calendar cal) {
        return startOfDay(cal).before(today());
    }

    /** Every day in the given month (1-12), each at local midnight, in ascending order. */
    @NonNull
    public static List<Calendar> daysInMonth(int year, int month) {
        List<Calendar> days = new ArrayList<>();
        Calendar cursor = calendarFor(year, month, 1);
        int count = cursor.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= count; day++) {
            days.add(calendarFor(year, month, day));
        }
        return days;
    }

    /** 0 (Sunday) .. 6 (Saturday): how many blank leading cells a Sun-start calendar grid needs. */
    public static int firstWeekdayOffset(int year, int month) {
        return calendarFor(year, month, 1).get(Calendar.DAY_OF_WEEK) - 1;
    }

    /** Adds {@code delta} months to (year, month), rolling the year over as needed. Returns {year, month}. */
    @NonNull
    public static int[] addMonths(int year, int month, int delta) {
        Calendar cal = calendarFor(year, month, 1);
        cal.add(Calendar.MONTH, delta);
        return new int[] {cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1};
    }

    // ===================== Display formatting =====================

    @NonNull
    public static String formatTime(long millis) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(millis));
    }

    @NonNull
    public static String formatShortDate(long millis) {
        return new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(millis));
    }

    @NonNull
    public static String formatLongDate(long millis) {
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date(millis));
    }

    @NonNull
    public static String formatMonthYear(int year, int month) {
        return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendarFor(year, month, 1).getTime());
    }

    @NonNull
    public static String formatDuration(long millis) {
        long totalMinutes = millis / 60_000L;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format(Locale.getDefault(), "%02dh %02dm", hours, minutes);
    }
}
