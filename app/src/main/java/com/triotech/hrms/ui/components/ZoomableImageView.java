package com.triotech.hrms.ui.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * An ImageView that supports pinch-to-zoom, double-tap zoom and panning, used by
 * the document/PDF viewer. Matrix-based so it works for both bitmap pages and
 * standalone images without a third-party library. When not zoomed in it reports
 * its scroll gestures back to the parent so a surrounding RecyclerView can still
 * scroll between pages.
 */
public class ZoomableImageView extends AppCompatImageView {

    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 5f;

    private final Matrix matrix = new Matrix();
    private final float[] values = new float[9];
    private final RectF drawableRect = new RectF();

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private boolean initialised;

    public ZoomableImageView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!initialised || changed) {
            fitCenter();
            initialised = true;
        }
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        initialised = false;
        requestLayout();
    }

    private void fitCenter() {
        Drawable d = getDrawable();
        if (d == null || d.getIntrinsicWidth() == 0) {
            return;
        }
        float viewW = getWidth();
        float viewH = getHeight();
        float drawW = d.getIntrinsicWidth();
        float drawH = d.getIntrinsicHeight();
        float scale = Math.min(viewW / drawW, viewH / drawH);
        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate((viewW - drawW * scale) / 2f, (viewH - drawH * scale) / 2f);
        setImageMatrix(matrix);
    }

    private float currentScale() {
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    private float baseScale() {
        Drawable d = getDrawable();
        if (d == null || d.getIntrinsicWidth() == 0) {
            return 1f;
        }
        return Math.min(getWidth() / (float) d.getIntrinsicWidth(),
                getHeight() / (float) d.getIntrinsicHeight());
    }

    private boolean isZoomed() {
        return currentScale() > baseScale() * 1.05f;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Let the parent (RecyclerView) handle vertical scroll unless we're zoomed in.
        getParent().requestDisallowInterceptTouchEvent(isZoomed() || event.getPointerCount() > 1);
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private void applyBounds() {
        Drawable d = getDrawable();
        if (d == null) {
            return;
        }
        drawableRect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        matrix.mapRect(drawableRect);

        float dx = 0;
        float dy = 0;
        float viewW = getWidth();
        float viewH = getHeight();

        if (drawableRect.width() <= viewW) {
            dx = (viewW - drawableRect.width()) / 2f - drawableRect.left;
        } else if (drawableRect.left > 0) {
            dx = -drawableRect.left;
        } else if (drawableRect.right < viewW) {
            dx = viewW - drawableRect.right;
        }

        if (drawableRect.height() <= viewH) {
            dy = (viewH - drawableRect.height()) / 2f - drawableRect.top;
        } else if (drawableRect.top > 0) {
            dy = -drawableRect.top;
        } else if (drawableRect.bottom < viewH) {
            dy = viewH - drawableRect.bottom;
        }

        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            float projected = currentScale() * factor;
            float min = baseScale();
            float max = baseScale() * MAX_SCALE;
            if (projected < min) {
                factor = min / currentScale();
            } else if (projected > max) {
                factor = max / currentScale();
            }
            matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
            applyBounds();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            if (isZoomed()) {
                fitCenter();
            } else {
                float target = baseScale() * 2.5f;
                float factor = target / currentScale();
                matrix.postScale(factor, factor, e.getX(), e.getY());
                applyBounds();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            if (isZoomed()) {
                matrix.postTranslate(-distanceX, -distanceY);
                applyBounds();
                return true;
            }
            return false;
        }
    }
}
