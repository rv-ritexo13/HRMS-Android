package com.triotech.hrms.ui.attendance;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.triotech.hrms.R;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.databinding.DialogAttendanceCorrectionBinding;
import java.util.Calendar;

/**
 * "Request an attendance correction" form: pick the day, the actual check-in
 * (or mark that none was recorded), the expected check-in, and a reason. The
 * dialog itself holds no repository/ViewModel reference — it hands the
 * finished request to its {@link Listener}, matching {@code ConfirmDialogFragment}'s
 * pattern so the hosting screen stays in control of the submit call and its
 * loading/error handling.
 */
public class AttendanceCorrectionDialogFragment extends DialogFragment {

    private static final String ARG_DATE_MILLIS = "arg_date_millis";
    private static final String ARG_ACTUAL_CHECK_IN_MILLIS = "arg_actual_check_in_millis";

    public interface Listener {
        void onCorrectionSubmit(
                @NonNull String dateKey,
                long dateMillis,
                @Nullable Long actualCheckInMillis,
                long expectedCheckInMillis,
                @NonNull String reason);
    }

    @Nullable private Listener listener;
    private DialogAttendanceCorrectionBinding binding;

    private Calendar selectedDate = DateUtils.today();
    @Nullable private Long actualCheckInMillis;
    private long expectedCheckInMillis;

    @NonNull
    public static AttendanceCorrectionDialogFragment newInstance(
            long defaultDateMillis, @Nullable Long defaultActualCheckInMillis) {
        AttendanceCorrectionDialogFragment fragment = new AttendanceCorrectionDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_DATE_MILLIS, defaultDateMillis);
        if (defaultActualCheckInMillis != null) {
            args.putLong(ARG_ACTUAL_CHECK_IN_MILLIS, defaultActualCheckInMillis);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public static void show(
            @NonNull FragmentManager fragmentManager,
            long defaultDateMillis,
            @Nullable Long defaultActualCheckInMillis,
            @NonNull Listener listener) {
        AttendanceCorrectionDialogFragment fragment = newInstance(defaultDateMillis, defaultActualCheckInMillis);
        fragment.setListener(listener);
        fragment.show(fragmentManager, "AttendanceCorrectionDialogFragment");
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        long defaultDateMillis = args.getLong(ARG_DATE_MILLIS, DateUtils.today().getTimeInMillis());
        selectedDate = DateUtils.startOfDay(DateUtils.calendarFromMillis(defaultDateMillis));
        actualCheckInMillis = args.containsKey(ARG_ACTUAL_CHECK_IN_MILLIS)
                ? args.getLong(ARG_ACTUAL_CHECK_IN_MILLIS)
                : null;
        expectedCheckInMillis = timeOn(selectedDate, 9, 30);

        binding = DialogAttendanceCorrectionBinding.inflate(LayoutInflater.from(requireContext()));

        renderDate();
        renderActualCheckIn();
        renderExpectedCheckIn();

        binding.textCorrectionDialogDate.setOnClickListener(v -> pickDate());
        binding.textCorrectionDialogActualCheckin.setOnClickListener(v -> pickActualCheckIn());
        binding.textCorrectionDialogExpectedCheckin.setOnClickListener(v -> pickExpectedCheckIn());
        binding.buttonClearActualCheckin.setOnClickListener(v -> {
            actualCheckInMillis = null;
            renderActualCheckIn();
        });

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .create();

        binding.buttonCorrectionSubmit.setOnClickListener(v -> attemptSubmit());

        return dialog;
    }

    private void attemptSubmit() {
        String reason = binding.editCorrectionReason.getText() != null
                ? binding.editCorrectionReason.getText().toString().trim()
                : "";
        binding.inputLayoutCorrectionReason.setError(null);
        if (TextUtils.isEmpty(reason)) {
            binding.inputLayoutCorrectionReason.setError(getString(R.string.attendance_correction_error_reason_required));
            return;
        }
        if (listener != null) {
            listener.onCorrectionSubmit(
                    DateUtils.dateKey(selectedDate), selectedDate.getTimeInMillis(), actualCheckInMillis,
                    expectedCheckInMillis, reason);
        }
        dismiss();
    }

    private void pickDate() {
        Calendar current = selectedDate;
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate = DateUtils.calendarFor(year, month + 1, dayOfMonth);
                    expectedCheckInMillis = timeOn(selectedDate, 9, 30);
                    if (actualCheckInMillis != null) {
                        Calendar previousTime = DateUtils.calendarFromMillis(actualCheckInMillis);
                        actualCheckInMillis = timeOn(
                                selectedDate, previousTime.get(Calendar.HOUR_OF_DAY), previousTime.get(Calendar.MINUTE));
                    }
                    renderDate();
                    renderExpectedCheckIn();
                    renderActualCheckIn();
                },
                current.get(Calendar.YEAR),
                current.get(Calendar.MONTH),
                current.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(DateUtils.today().getTimeInMillis());
        dialog.show();
    }

    private void pickActualCheckIn() {
        Calendar base = actualCheckInMillis != null
                ? DateUtils.calendarFromMillis(actualCheckInMillis)
                : timeOnCalendar(selectedDate, 9, 30);
        new TimePickerDialog(
                        requireContext(),
                        (view, hour, minute) -> {
                            actualCheckInMillis = timeOn(selectedDate, hour, minute);
                            renderActualCheckIn();
                        },
                        base.get(Calendar.HOUR_OF_DAY),
                        base.get(Calendar.MINUTE),
                        false)
                .show();
    }

    private void pickExpectedCheckIn() {
        Calendar base = DateUtils.calendarFromMillis(expectedCheckInMillis);
        new TimePickerDialog(
                        requireContext(),
                        (view, hour, minute) -> {
                            expectedCheckInMillis = timeOn(selectedDate, hour, minute);
                            renderExpectedCheckIn();
                        },
                        base.get(Calendar.HOUR_OF_DAY),
                        base.get(Calendar.MINUTE),
                        false)
                .show();
    }

    private void renderDate() {
        binding.textCorrectionDialogDate.setText(DateUtils.formatLongDate(selectedDate.getTimeInMillis()));
    }

    private void renderActualCheckIn() {
        binding.textCorrectionDialogActualCheckin.setText(actualCheckInMillis != null
                ? DateUtils.formatTime(actualCheckInMillis)
                : getString(R.string.attendance_correction_no_checkin));
    }

    private void renderExpectedCheckIn() {
        binding.textCorrectionDialogExpectedCheckin.setText(DateUtils.formatTime(expectedCheckInMillis));
    }

    private static long timeOn(@NonNull Calendar day, int hour, int minute) {
        return timeOnCalendar(day, hour, minute).getTimeInMillis();
    }

    @NonNull
    private static Calendar timeOnCalendar(@NonNull Calendar day, int hour, int minute) {
        Calendar cal = (Calendar) day.clone();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }
}
