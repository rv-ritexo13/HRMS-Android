package com.triotech.hrms.ui.components;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.triotech.hrms.R;
import com.triotech.hrms.databinding.DialogConfirmBinding;

/**
 * Design-system confirmation dialog.
 *
 * <p>Wraps a themed {@link MaterialAlertDialogBuilder} around the shared
 * {@code dialog_confirm.xml} layout so every "are you sure?" moment in the
 * app (logout, discard changes, destructive actions in later phases) looks
 * and behaves the same way.</p>
 */
public class ConfirmDialogFragment extends DialogFragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_MESSAGE = "arg_message";
    private static final String ARG_ICON = "arg_icon";
    private static final String ARG_POSITIVE_TEXT = "arg_positive_text";
    private static final String ARG_NEGATIVE_TEXT = "arg_negative_text";

    /** Implemented by the host Fragment/Activity to react to the user's choice. */
    public interface Listener {
        void onConfirmed(@NonNull String requestKey);
    }

    private String requestKey = "";
    @Nullable private Listener listener;

    public static ConfirmDialogFragment newInstance(
            @NonNull String requestKey,
            @StringRes int title,
            @StringRes int message,
            @DrawableRes int icon,
            @StringRes int positiveText,
            @StringRes int negativeText) {
        ConfirmDialogFragment fragment = new ConfirmDialogFragment();
        Bundle args = new Bundle();
        args.putString("request_key", requestKey);
        args.putInt(ARG_TITLE, title);
        args.putInt(ARG_MESSAGE, message);
        args.putInt(ARG_ICON, icon);
        args.putInt(ARG_POSITIVE_TEXT, positiveText);
        args.putInt(ARG_NEGATIVE_TEXT, negativeText);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        requestKey = args.getString("request_key", "");

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        DialogConfirmBinding binding = DialogConfirmBinding.inflate(inflater);

        binding.imageDialogIcon.setImageResource(args.getInt(ARG_ICON, R.drawable.ic_info));
        binding.textDialogTitle.setText(args.getInt(ARG_TITLE));
        binding.textDialogMessage.setText(args.getInt(ARG_MESSAGE));
        binding.buttonDialogPositive.setText(args.getInt(ARG_POSITIVE_TEXT, R.string.action_confirm));
        binding.buttonDialogNegative.setText(args.getInt(ARG_NEGATIVE_TEXT, R.string.action_cancel));

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .create();

        binding.buttonDialogPositive.setOnClickListener((View v) -> {
            if (listener != null) {
                listener.onConfirmed(requestKey);
            }
            dismiss();
        });
        binding.buttonDialogNegative.setOnClickListener((View v) -> dismiss());

        return dialog;
    }

    /** Convenience shortcut used by screens that just need a fire-and-forget confirm dialog. */
    public static void show(
            @NonNull FragmentManager fragmentManager,
            @NonNull String requestKey,
            @StringRes int title,
            @StringRes int message,
            @DrawableRes int icon,
            @StringRes int positiveText,
            @StringRes int negativeText,
            @NonNull Listener listener) {
        ConfirmDialogFragment fragment =
                newInstance(requestKey, title, message, icon, positiveText, negativeText);
        fragment.setListener(listener);
        fragment.show(fragmentManager, "ConfirmDialogFragment:" + requestKey);
    }
}
