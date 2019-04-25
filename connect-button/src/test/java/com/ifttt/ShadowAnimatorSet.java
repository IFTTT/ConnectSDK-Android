package com.ifttt;

import android.animation.AnimatorSet;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

/**
 * Custom Shadow that ends immediately after starting. Used for testing state changes when there is animation involved.
 */
@Implements(AnimatorSet.class)
public class ShadowAnimatorSet {

    @RealObject private AnimatorSet realAnimator;

    @Implementation
    public void start() {
        realAnimator.end();
    }
}
