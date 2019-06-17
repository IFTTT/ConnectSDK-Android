package com.ifttt.connect.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;
import com.ifttt.connect.R;
import javax.annotation.CheckReturnValue;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;

/**
 * Custom view used to handle dragging gesture with a {@link ViewDragHelper} as well as setting maximum width for the
 * {@link BaseConnectButton}.
 */
final class ButtonParentView extends FrameLayout {

    private final int maxWidth = getResources().getDimensionPixelSize(R.dimen.ifttt_connect_button_width);
    private final int minWidth = getResources().getDimensionPixelSize(R.dimen.ifttt_connect_button_min_width);

    private ViewDragHelper helper;

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
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int offset = ((LayoutParams) child.getLayoutParams()).horizontalOffset;
            ViewCompat.offsetLeftAndRight(child, offset);
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
        ((LayoutParams) target.getLayoutParams()).horizontalOffset = left;
    }

    @Override
    protected FrameLayout.LayoutParams generateDefaultLayoutParams() {
        if (SDK_INT >= KITKAT) {
            return new LayoutParams(super.generateDefaultLayoutParams());
        }

        return new LayoutParams((MarginLayoutParams) super.generateDefaultLayoutParams());
    }

    @Override
    public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (SDK_INT >= KITKAT && lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        } else if (SDK_INT >= KITKAT && lp instanceof FrameLayout.LayoutParams) {
            return new LayoutParams((FrameLayout.LayoutParams) lp);
        } else if (lp instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) lp);
        } else {
            return new LayoutParams(lp);
        }
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

    private static final class LayoutParams extends FrameLayout.LayoutParams {

        int horizontalOffset;

        LayoutParams(@NonNull Context c, @Nullable AttributeSet attrs) {
            super(c, attrs);
        }

        LayoutParams(@NonNull ViewGroup.LayoutParams source) {
            super(source);
        }

        LayoutParams(@NonNull MarginLayoutParams source) {
            super(source);
        }

        @RequiresApi(api = KITKAT)
        LayoutParams(@NonNull FrameLayout.LayoutParams source) {
            super(source);
        }

        @RequiresApi(api = KITKAT)
        LayoutParams(@NonNull LayoutParams source) {
            super(source);

            this.horizontalOffset = source.horizontalOffset;
        }
    }
}
