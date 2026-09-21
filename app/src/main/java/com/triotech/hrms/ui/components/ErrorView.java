package com.triotech.hrms.ui.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.triotech.hrms.R;
import com.triotech.hrms.databinding.ViewErrorBinding;

/**
 * Design-system error state: icon + title + message + retry action.
 *
 * <p>Customizable from XML via {@code app:stateIcon}, {@code app:stateTitle},
 * {@code app:stateMessage} and {@code app:stateButtonText}, or programmatically
 * via the setters below. Attach a retry callback with {@link #setOnRetryListener}.</p>
 */
public class ErrorView extends FrameLayout {

    private final ViewErrorBinding binding;

    public interface OnRetryListener {
        void onRetry();
    }

    public ErrorView(@NonNull Context context) {
        this(context, null);
    }

    public ErrorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ErrorView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        binding = ViewErrorBinding.inflate(LayoutInflater.from(context), this);

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

    public void setOnRetryListener(@Nullable OnRetryListener listener) {
        binding.buttonStateAction.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRetry();
            }
        });
    }
}
