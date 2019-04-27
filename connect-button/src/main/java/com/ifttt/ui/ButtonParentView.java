package com.ifttt.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;
import com.ifttt.R;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.CheckReturnValue;

/**
 * Custom view used to handle dragging gesture with a {@link ViewDragHelper} as well as setting maximum width for the
 * {@link BaseConnectButton}.
 */
final class ButtonParentView extends FrameLayout {

    private final int maxWidth = getResources().getDimensionPixelSize(R.dimen.ifttt_connect_button_width);
    private final int minWidth = getResources().getDimensionPixelSize(R.dimen.ifttt_connect_button_min_width);

    private ViewDragHelper helper;

    private final Map<View, Integer> viewOffsets = new LinkedHashMap<>();

    public ButtonParentView(@NonNull Context context) {
        super(context);
    }

    public ButtonParentView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonParentView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width > maxWidth) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
        } else if (width < minWidth) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(minWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // Re-apply all targeted views' horizontal offset.
        for (Map.Entry<View, Integer> entry : viewOffsets.entrySet()) {
            ViewCompat.offsetLeftAndRight(entry.getKey(), entry.getValue());
        }
    }

    @CheckReturnValue
    ViewDragHelper getViewDragHelperCallback(ViewDragHelper.Callback callback) {
        helper = ViewDragHelper.create(this, callback);
        return helper;
    }

    /**
     * Keep track of the left and right offset for the target view.
     *
     * @param target View that's offset by {@link ViewCompat#offsetLeftAndRight(View, int)}.
     * @param left Offset pixels that is applied to the target view.
     */
    void trackViewLeftAndRightOffset(View target, int left) {
        viewOffsets.put(target, left);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (helper == null) {
            throw new IllegalStateException("Call getViewDragHelperCallback first.");
        }

        if (helper.shouldInterceptTouchEvent(ev)) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (helper == null) {
            throw new IllegalStateException("Call getViewDragHelperCallback first.");
        }
        helper.processTouchEvent(event);
        return super.onTouchEvent(event);
    }
}
