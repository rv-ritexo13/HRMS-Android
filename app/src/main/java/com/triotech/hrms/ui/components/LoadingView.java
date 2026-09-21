package com.triotech.hrms.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.triotech.hrms.R;
import com.triotech.hrms.databinding.ViewLoadingBinding;

/**
 * Design-system loading state: a themed indeterminate spinner with an optional message.
 *
 * <p>Usage in a layout:</p>
 * <pre>
 * &lt;com.triotech.hrms.ui.components.LoadingView
 *     android:id="@+id/loadingView"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent" /&gt;
 * </pre>
 */
public class LoadingView extends FrameLayout {

    private final ViewLoadingBinding binding;

    public LoadingView(@NonNull Context context) {
        this(context, null);
    }

    public LoadingView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        binding = ViewLoadingBinding.inflate(android.view.LayoutInflater.from(context), this);

        if (attrs != null) {
            android.content.res.TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StateView);
            try {
                String message = a.getString(R.styleable.StateView_stateMessage);
                if (message != null) {
                    binding.textLoadingMessage.setText(message);
                }
            } finally {
                a.recycle();
            }
        }
    }

    public void setMessage(@NonNull String message) {
        binding.textLoadingMessage.setText(message);
    }

    public void setMessage(@StringRes int messageRes) {
        binding.textLoadingMessage.setText(messageRes);
    }
}
