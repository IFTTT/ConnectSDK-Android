package com.ifttt.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.customview.widget.ViewDragHelper;
import javax.annotation.CheckReturnValue;

/**
 * Custom view used to handle dragging gesture with a {@link ViewDragHelper}.
 */
final class DragParentView extends FrameLayout {

    private ViewDragHelper helper;

    public DragParentView(@NonNull Context context) {
        super(context);
    }

    public DragParentView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DragParentView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @CheckReturnValue
    ViewDragHelper getViewDragHelperCallback(ViewDragHelper.Callback callback) {
        helper = ViewDragHelper.create(this, callback);
        return helper;
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
