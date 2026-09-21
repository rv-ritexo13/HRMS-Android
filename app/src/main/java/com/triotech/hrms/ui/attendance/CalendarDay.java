package com.triotech.hrms.ui.attendance;

import androidx.annotation.Nullable;
import com.triotech.hrms.data.model.AttendanceStatus;

/** One cell in the Attendance Calendar's 7-column grid — a real day, or a leading blank filler. */
class CalendarDay {

    final boolean isBlank;
    final int dayOfMonth;
    final long dateMillis;
    final boolean isToday;
    final boolean isWeekend;
    @Nullable final AttendanceStatus status;

    private CalendarDay(
            boolean isBlank, int dayOfMonth, long dateMillis, boolean isToday, boolean isWeekend,
            @Nullable AttendanceStatus status) {
        this.isBlank = isBlank;
        this.dayOfMonth = dayOfMonth;
        this.dateMillis = dateMillis;
        this.isToday = isToday;
        this.isWeekend = isWeekend;
        this.status = status;
    }

    @androidx.annotation.NonNull
    static CalendarDay blank(int slotIndex) {
        return new CalendarDay(true, 0, -1L - slotIndex, false, false, null);
    }

    @androidx.annotation.NonNull
    static CalendarDay of(
            int dayOfMonth, long dateMillis, boolean isToday, boolean isWeekend, @Nullable AttendanceStatus status) {
        return new CalendarDay(false, dayOfMonth, dateMillis, isToday, isWeekend, status);
    }
}
