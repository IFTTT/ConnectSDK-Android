package com.ifttt.connect.ui;

import android.animation.ArgbEvaluator;
import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import javax.annotation.Nullable;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;

/**
 * Backwards compatibility version of the {@link ProgressBackground} for Jelly Bean. It doesn't render the
 */
@TargetApi(JELLY_BEAN_MR2)
final class ProgressBackgroundJellyBean extends Drawable implements ProgressBackground {
    private static final ArgbEvaluator EVALUATOR = new ArgbEvaluator();

    private final ShapeDrawable drawable = new ShapeDrawable();

    private int primaryColor;
    private int progressColor;

    @Override
    public void setColor(@ColorInt int primaryColor, @ColorInt int progressColor) {
        this.primaryColor = primaryColor;
        this.progressColor = progressColor;
        invalidateSelf();
    }

    @Override
    public void setProgress(@FloatRange(from = 0.0f, to = 1.0f) float progress) {
        drawable.getPaint().setColor((Integer) EVALUATOR.evaluate(progress, primaryColor, progressColor));
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        drawable.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        drawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        drawable.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        if (!drawable.getBounds().equals(bounds)) {
            float radius = bounds.height() / 2f;
            float[] radii = new float[] { radius, radius, radius, radius, radius, radius, radius, radius };
            drawable.setShape(new RoundRectShape(radii, null, null));
            drawable.setBounds(bounds);
        }
    }
}
