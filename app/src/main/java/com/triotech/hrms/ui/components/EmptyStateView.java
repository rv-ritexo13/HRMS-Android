package com.triotech.hrms.ui.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.triotech.hrms.R;
import com.triotech.hrms.databinding.ViewEmptyStateBinding;

/**
 * Design-system empty state: icon + title + message, with an optional call-to-action
 * button (hidden by default; call {@link #setOnActionListener} to reveal it).
 */
public class EmptyStateView extends FrameLayout {

    private final ViewEmptyStateBinding binding;

    public interface OnActionListener {
        void onAction();
    }

    public EmptyStateView(@NonNull Context context) {
        this(context, null);
    }

    public EmptyStateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmptyStateView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        binding = ViewEmptyStateBinding.inflate(LayoutInflater.from(context), this);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StateView);
            try {
                int icon = a.getResourceId(R.styleable.StateView_stateIcon, 0);
                if (icon != 0) {
                    binding.imageStateIcon.setImageResource(icon);
                }
                String title = a.getString(R.styleable.StateView_stateTitle);
                if (title != null) {
                    binding.textStateTitle.setText(title);
                }
                String message = a.getString(R.styleable.StateView_stateMessage);
                if (message != null) {
                    binding.textStateMessage.setText(message);
                }
                String buttonText = a.getString(R.styleable.StateView_stateButtonText);
                if (buttonText != null) {
                    binding.buttonStateAction.setText(buttonText);
                }
                boolean showButton = a.getBoolean(R.styleable.StateView_stateShowButton, false);
                binding.buttonStateAction.setVisibility(showButton ? View.VISIBLE : View.GONE);
            } finally {
                a.recycle();
            }
        }
    }

    public void setTitle(@StringRes int titleRes) {
        binding.textStateTitle.setText(titleRes);
    }

    public void setMessage(@StringRes int messageRes) {
        binding.textStateMessage.setText(messageRes);
    }

    public void setIcon(@DrawableRes int iconRes) {
        binding.imageStateIcon.setImageResource(iconRes);
    }

    public void setOnActionListener(@Nullable OnActionListener listener) {
        binding.buttonStateAction.setVisibility(listener != null ? View.VISIBLE : View.GONE);
        binding.buttonStateAction.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAction();
            }
        });
    }
}
