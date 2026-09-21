package com.triotech.hrms.ui.components;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.triotech.hrms.databinding.DialogSuccessBinding;

/**
 * Design-system success confirmation: icon + title + a prominent primary value
 * (e.g. a timestamp) + a short subtitle, dismissed with a single "Done" button.
 * Used for the Check In / Check Out confirmations
 * ("✓ Checked In Successfully" / "09:32 AM" / "Have a productive day!"), and
 * reusable anywhere else a lightweight success moment is needed.
 */
public class SuccessDialogFragment extends DialogFragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_PRIMARY_VALUE = "arg_primary_value";
    private static final String ARG_SUBTITLE = "arg_subtitle";

    public static void show(
            @NonNull FragmentManager fragmentManager,
            @NonNull String tag,
            @NonNull String title,
            @NonNull String primaryValue,
            @NonNull String subtitle) {
        SuccessDialogFragment fragment = new SuccessDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_PRIMARY_VALUE, primaryValue);
        args.putString(ARG_SUBTITLE, subtitle);
        fragment.setArguments(args);
        fragment.show(fragmentManager, tag);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        DialogSuccessBinding binding = DialogSuccessBinding.inflate(inflater);

        binding.textSuccessTitle.setText(args.getString(ARG_TITLE));
        binding.textSuccessPrimaryValue.setText(args.getString(ARG_PRIMARY_VALUE));
        binding.textSuccessSubtitle.setText(args.getString(ARG_SUBTITLE));

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .setCancelable(true)
                .create();

        binding.buttonSuccessDone.setOnClickListener(v -> dismiss());
        return dialog;
    }
}
