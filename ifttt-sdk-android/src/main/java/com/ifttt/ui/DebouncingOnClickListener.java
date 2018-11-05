package com.ifttt.ui;

import android.view.View;

/**
 * A {@linkplain View.OnClickListener click listener} that debounces multiple clicks posted in the
 * same frame. A click on one button disables all buttons for that frame.
 *
 * From https://github.com/JakeWharton/butterknife/blob/master/butterknife-runtime/src/main/java/butterknife/internal/DebouncingOnClickListener.java
 */
abstract class DebouncingOnClickListener implements View.OnClickListener {

    private static boolean ENABLED = true;
    private static final Runnable ENABLE_AGAIN = () -> ENABLED = true;

    @Override
    public final void onClick(View v) {
        if (ENABLED) {
            ENABLED = false;
            v.post(ENABLE_AGAIN);
            doClick(v);
        }
    }

    abstract void doClick(View v);
}
