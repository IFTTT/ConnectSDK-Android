package com.ifttt.ui;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;

interface ProgressBackground {

    /**
     * Set the current progress that should be rendered.
     *
     * @param progress a progress value ranging from 0 to 1.
     */
    void setProgress(@FloatRange(from = 0.0f, to = 1.0f) float progress);

    /**
     * Set up the drawable as a progress bar.
     *
     * @param primaryColor Primary color of the drawable.
     * @param progressColor Progress bar color of the drawable.
     */
    void setColor(@ColorInt int primaryColor, @ColorInt int progressColor);
}
