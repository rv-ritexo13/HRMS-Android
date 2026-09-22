package com.triotech.hrms.ui.attendance;

import android.Manifest;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;
import com.triotech.hrms.R;
import com.triotech.hrms.core.base.BaseFragment;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.LocationUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.AttendanceCorrectionRequest;
import com.triotech.hrms.data.model.AttendanceHistoryEntry;
import com.triotech.hrms.data.model.AttendanceRecord;
import com.triotech.hrms.data.model.AttendanceStatus;
import com.triotech.hrms.data.model.MonthlyAttendanceSummary;
import com.triotech.hrms.data.repository.AttendanceRepository;
import com.triotech.hrms.databinding.FragmentAttendanceBinding;
import com.triotech.hrms.databinding.ItemAttendanceSummaryStatBinding;
import com.triotech.hrms.ui.components.SuccessDialogFragment;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The full Attendance module: today's live check-in/check-out card, a
 * shared-month Monthly Summary + Calendar + History (all three stay in sync),
 * and attendance correction requests. GPS/geofencing/location tracking are
 * explicitly out of scope for this phase — everything here is date/time based,
 * backed by {@link AttendanceRepository}'s mock data (see
 * {@code FakeAttendanceRepository} for how the check-in/check-out edge cases —
 * double check-in, check-out before check-in, a missing check-out surviving an
 * app restart — are handled).
 */
public class AttendanceFragment extends BaseFragment<FragmentAttendanceBinding>
        implements AttendanceCorrectionDialogFragment.Listener {

    private AttendanceViewModel viewModel;

    private final CalendarDayAdapter calendarAdapter = new CalendarDayAdapter();
    private final AttendanceHistoryAdapter historyAdapter = new AttendanceHistoryAdapter();
    private final CorrectionAdapter correctionAdapter = new CorrectionAdapter();

    // Registered at construction (required for ActivityResult APIs). Whatever the
    // permission result, check-in proceeds — location is recorded if granted, skipped if not.
    private final ActivityResultLauncher<String[]> locationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> performCheckIn());

    @Nullable private int[] currentSelectedMonth;
    @Nullable private List<AttendanceHistoryEntry> currentHistoryEntries;
    @Nullable private Long pendingMissingCheckoutCheckIn;

    @Override
    protected FragmentAttendanceBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAttendanceBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        AttendanceRepository repository = ServiceLocator.getInstance().getAttendanceRepository();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(() -> new AttendanceViewModel(repository)))
                .get(AttendanceViewModel.class);

        setupWeekdayHeader();
        setupRecyclerViews();

        getBinding().buttonCheckIn.setOnClickListener(v -> requestCheckIn());
        getBinding().buttonCheckOut.setOnClickListener(v -> performCheckOut());
        getBinding().buttonPreviousMonth.setOnClickListener(v -> viewModel.goToPreviousMonth());
        getBinding().buttonNextMonth.setOnClickListener(v -> viewModel.goToNextMonth());
        getBinding().buttonRequestCorrection.setOnClickListener(v -> openCorrectionDialog(DateUtils.today().getTimeInMillis(), null));
        getBinding().buttonMissingCheckoutAction.setOnClickListener(v -> {
            String dateKey = viewModel.getPendingMissingCheckout().getValue();
            if (dateKey != null) {
                Calendar day = parseDateKeyToCalendar(dateKey);
                openCorrectionDialog(day.getTimeInMillis(), pendingMissingCheckoutCheckIn);
            }
        });

        viewModel.getToday().observe(getViewLifecycleOwner(), this::renderAttendance);
        viewModel.getPendingMissingCheckout().observe(getViewLifecycleOwner(), this::renderMissingCheckoutBanner);
        viewModel.getSelectedMonth().observe(getViewLifecycleOwner(), month -> {
            currentSelectedMonth = month;
            getBinding().textSelectedMonth.setText(DateUtils.formatMonthYear(month[0], month[1]));
            Calendar now = DateUtils.today();
            boolean isCurrentMonth = month[0] == now.get(Calendar.YEAR) && month[1] == now.get(Calendar.MONTH) + 1;
            getBinding().buttonNextMonth.setEnabled(!isCurrentMonth);
            getBinding().buttonNextMonth.setAlpha(isCurrentMonth ? 0.4f : 1f);
            renderCalendarIfReady();
        });
        viewModel.getSummary().observe(getViewLifecycleOwner(), this::renderSummary);
        viewModel.getHistory().observe(getViewLifecycleOwner(), resource -> {
            renderHistory(resource);
            currentHistoryEntries = resource.isSuccess() ? resource.data : new ArrayList<>();
            renderCalendarIfReady();
        });
        viewModel.getCorrections().observe(getViewLifecycleOwner(), this::renderCorrections);
    }

    private void setupWeekdayHeader() {
        String[] labels = getResources().getStringArray(R.array.attendance_calendar_weekday_labels);
        int labelColor = MaterialColors.getColor(
                getBinding().rowCalendarWeekdays, com.google.android.material.R.attr.colorOnSurfaceVariant);
        getBinding().rowCalendarWeekdays.removeAllViews();
        for (String label : labels) {
            TextView textView = new TextView(requireContext());
            textView.setText(label);
            textView.setGravity(Gravity.CENTER);
            textView.setTextAppearance(R.style.TextAppearance_HRMS_LabelSmall);
            textView.setTextColor(labelColor);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textView.setLayoutParams(params);
            getBinding().rowCalendarWeekdays.addView(textView);
        }
    }

    private void setupRecyclerViews() {
        getBinding().recyclerCalendarDays.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        getBinding().recyclerCalendarDays.setAdapter(calendarAdapter);

        getBinding().recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().recyclerHistory.setAdapter(historyAdapter);

        getBinding().recyclerCorrections.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().recyclerCorrections.setAdapter(correctionAdapter);
    }

    // ===================== Today: check-in / check-out =====================

    /** Asks for location permission (once) before checking in; check-in is never blocked by the answer. */
    private void requestCheckIn() {
        if (LocationUtils.hasPermission(requireContext())) {
            performCheckIn();
        } else {
            locationPermLauncher.launch(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void performCheckIn() {
        viewModel.checkIn().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isError() && resource.message != null) {
                showSnackbar(resource.message);
            } else if (resource.isSuccess() && resource.data != null && resource.data.getCheckInTimeMillis() != null) {
                SuccessDialogFragment.show(
                        getChildFragmentManager(),
                        "checkin_success",
                        getString(R.string.attendance_check_in_success_title),
                        DateUtils.formatTime(resource.data.getCheckInTimeMillis()),
                        getString(R.string.attendance_check_in_success_subtitle));
            }
        });
    }

    private void performCheckOut() {
        viewModel.checkOut().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isError() && resource.message != null) {
                showSnackbar(resource.message);
            } else if (resource.isSuccess() && resource.data != null && resource.data.getCheckOutTimeMillis() != null) {
                SuccessDialogFragment.show(
                        getChildFragmentManager(),
                        "checkout_success",
                        getString(R.string.attendance_check_out_success_title),
                        DateUtils.formatTime(resource.data.getCheckOutTimeMillis()),
                        getString(R.string.attendance_check_out_success_subtitle_format,
                                DateUtils.formatDuration(resource.data.getWorkingDurationMillis())));
            }
        });
    }

    private void renderAttendance(@NonNull AttendanceRecord record) {
        int statusLabelRes;
        switch (record.getStatus()) {
            case CHECKED_IN:
                statusLabelRes = R.string.dashboard_status_checked_in;
                break;
            case CHECKED_OUT:
                statusLabelRes = R.string.dashboard_status_checked_out;
                break;
            case NOT_CHECKED_IN:
            default:
                statusLabelRes = R.string.dashboard_status_not_checked_in;
                break;
        }
        getBinding().chipAttendanceStatus.setText(statusLabelRes);
        applyStatusChipColor(record.getStatus());

        getBinding().textCheckInTime.setText(record.getCheckInTimeMillis() != null
                ? DateUtils.formatTime(record.getCheckInTimeMillis())
                : getString(R.string.dashboard_attendance_time_placeholder));
        getBinding().textCheckOutTime.setText(record.getCheckOutTimeMillis() != null
                ? DateUtils.formatTime(record.getCheckOutTimeMillis())
                : getString(R.string.dashboard_attendance_time_placeholder));

        if (record.getStatus() == AttendanceRecord.Status.CHECKED_OUT) {
            getBinding().textWorkingHours.setText(DateUtils.formatDuration(record.getWorkingDurationMillis()));
        } else if (record.getStatus() == AttendanceRecord.Status.CHECKED_IN) {
            getBinding().textWorkingHours.setText(R.string.dashboard_attendance_hours_in_progress);
        } else {
            getBinding().textWorkingHours.setText(R.string.dashboard_attendance_hours_placeholder);
        }

        getBinding().buttonCheckIn.setEnabled(record.canCheckIn());
        getBinding().buttonCheckOut.setEnabled(record.canCheckOut());
    }

    private void applyStatusChipColor(@NonNull AttendanceRecord.Status status) {
        int textColorRes;
        int bgColorRes;
        switch (status) {
            case CHECKED_IN:
                textColorRes = R.color.hrms_status_success;
                bgColorRes = R.color.hrms_status_success_container;
                break;
            case CHECKED_OUT:
                textColorRes = R.color.hrms_status_info;
                bgColorRes = R.color.hrms_status_info_container;
                break;
            case NOT_CHECKED_IN:
            default:
                textColorRes = R.color.hrms_status_warning;
                bgColorRes = R.color.hrms_status_warning_container;
                break;
        }
        getBinding().chipAttendanceStatus.setTextColor(ContextCompat.getColor(requireContext(), textColorRes));
        getBinding().chipAttendanceStatus.setChipBackgroundColor(
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bgColorRes)));
    }

    // ===================== Missing check-out banner =====================

    private void renderMissingCheckoutBanner(@Nullable String dateKey) {
        if (dateKey == null) {
            getBinding().cardMissingCheckout.setVisibility(View.GONE);
            pendingMissingCheckoutCheckIn = null;
            return;
        }
        Calendar day = parseDateKeyToCalendar(dateKey);
        getBinding().textMissingCheckout.setText(
                getString(R.string.attendance_missing_checkout_banner_format, DateUtils.formatLongDate(day.getTimeInMillis())));
        getBinding().cardMissingCheckout.setVisibility(View.VISIBLE);
    }

    // ===================== Monthly summary =====================

    private void renderSummary(@NonNull Resource<MonthlyAttendanceSummary> resource) {
        if (!resource.isSuccess() || resource.data == null) {
            return;
        }
        MonthlyAttendanceSummary summary = resource.data;
        bindSummaryStat(getBinding().statPresent, AttendanceStatus.PRESENT, summary.getPresentDays());
        bindSummaryStat(getBinding().statLate, AttendanceStatus.LATE, summary.getLateDays());
        bindSummaryStat(getBinding().statHalfDay, AttendanceStatus.HALF_DAY, summary.getHalfDays());
        bindSummaryStat(getBinding().statAbsent, AttendanceStatus.ABSENT, summary.getAbsentDays());
        bindSummaryStat(getBinding().statLeave, AttendanceStatus.LEAVE, summary.getLeaveDays());
        bindSummaryStat(getBinding().statWfh, AttendanceStatus.WORK_FROM_HOME, summary.getWorkFromHomeDays());
    }

    private void bindSummaryStat(
            @NonNull ItemAttendanceSummaryStatBinding stat, @NonNull AttendanceStatus status, int count) {
        stat.textSummaryStatValue.setText(String.valueOf(count));
        stat.textSummaryStatLabel.setText(AttendanceStatusPresenter.labelRes(status));
        stat.dotSummaryStat.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), AttendanceStatusPresenter.colorRes(status))));
    }

    // ===================== Calendar =====================

    private void renderCalendarIfReady() {
        if (currentSelectedMonth == null) {
            return;
        }
        int year = currentSelectedMonth[0];
        int month = currentSelectedMonth[1];

        Map<String, AttendanceStatus> statusByDate = new HashMap<>();
        if (currentHistoryEntries != null) {
            for (AttendanceHistoryEntry entry : currentHistoryEntries) {
                statusByDate.put(entry.getDateKey(), entry.getStatus());
            }
        }

        List<CalendarDay> days = new ArrayList<>();
        int leadingBlanks = DateUtils.firstWeekdayOffset(year, month);
        for (int i = 0; i < leadingBlanks; i++) {
            days.add(CalendarDay.blank(i));
        }
        Calendar today = DateUtils.today();
        for (Calendar day : DateUtils.daysInMonth(year, month)) {
            boolean isToday = DateUtils.isSameDay(day, today);
            boolean isWeekend = DateUtils.isWeekend(day);
            AttendanceStatus status = statusByDate.get(DateUtils.dateKey(day));
            days.add(CalendarDay.of(day.get(Calendar.DAY_OF_MONTH), day.getTimeInMillis(), isToday, isWeekend, status));
        }
        calendarAdapter.submitList(days);
    }

    // ===================== History =====================

    private void renderHistory(@NonNull Resource<List<AttendanceHistoryEntry>> resource) {
        getBinding().progressHistory.setVisibility(resource.isLoading() ? View.VISIBLE : View.GONE);
        getBinding().recyclerHistory.setVisibility(resource.isSuccess() ? View.VISIBLE : View.GONE);

        boolean showEmpty = resource.isEmpty() || resource.isError();
        getBinding().textHistoryEmpty.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        if (resource.isError()) {
            getBinding().textHistoryEmpty.setText(R.string.attendance_history_error);
        } else if (resource.isEmpty()) {
            getBinding().textHistoryEmpty.setText(R.string.attendance_history_empty);
        }

        if (resource.isSuccess() && resource.data != null) {
            historyAdapter.submitList(resource.data);
        }
    }

    // ===================== Corrections =====================

    private void openCorrectionDialog(long defaultDateMillis, @Nullable Long defaultActualCheckInMillis) {
        AttendanceCorrectionDialogFragment.show(
                getChildFragmentManager(), defaultDateMillis, defaultActualCheckInMillis, this);
    }

    @Override
    public void onCorrectionSubmit(
            @NonNull String dateKey,
            long dateMillis,
            @Nullable Long actualCheckInMillis,
            long expectedCheckInMillis,
            @NonNull String reason) {
        viewModel.submitCorrection(dateKey, dateMillis, actualCheckInMillis, expectedCheckInMillis, reason)
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource.isSuccess()) {
                        showSnackbar(getString(R.string.attendance_correction_success));
                    } else if (resource.isError() && resource.message != null) {
                        showSnackbar(resource.message);
                    }
                });
    }

    private void renderCorrections(@NonNull Resource<List<AttendanceCorrectionRequest>> resource) {
        boolean hasItems = resource.isSuccess() && resource.data != null && !resource.data.isEmpty();
        getBinding().recyclerCorrections.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        getBinding().textCorrectionsEmpty.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        if (hasItems) {
            correctionAdapter.submitList(resource.data);
        }
    }

    // ===================== Helpers =====================

    @NonNull
    private static Calendar parseDateKeyToCalendar(@NonNull String dateKey) {
        String[] parts = dateKey.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);
        return DateUtils.calendarFor(year, month, day);
    }

    private void showSnackbar(@NonNull String message) {
        Snackbar.make(getBinding().getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}
