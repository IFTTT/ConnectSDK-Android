package com.ifttt.ui;

import android.view.animation.Interpolator;

/**
 * Customized Interpolator to gain a smoother animation.
 */
final class SmoothInterpolator implements Interpolator {
    @Override
    public float getInterpolation(float input) {
        return (float) (Math.pow((input - 1), 5) + 1);
    }
}
