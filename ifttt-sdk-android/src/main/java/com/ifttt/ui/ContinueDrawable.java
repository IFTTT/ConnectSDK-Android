package com.ifttt.ui;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

/**
 * Custom drawable that can play a repeating translation animation.
 */
final class ContinueDrawable extends Drawable {

    private static final float SCALE_FACTOR = 0.5f;
    private static final float IMAGE_MOVEMENT_TRANSLATION_X = 10; // px
    private static final long ANIMATION_DURATION = 1200L;

    private final Drawable image;
    private final Paint circlePaint;

    private float translationX = 0f;

    /**
     * @param image Drawable that will be animated.
     * @param backgroundColor A color value used to render a circle background.
     */
    ContinueDrawable(Drawable image, @ColorInt int backgroundColor) {
        this.image = image;

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(backgroundColor);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();

        canvas.drawOval(new RectF(0, 0, bounds.right, bounds.bottom), circlePaint);

        int countScale = canvas.save();
        canvas.translate(translationX, 0);
        image.draw(canvas);
        canvas.restoreToCount(countScale);
    }

    @Override
    public void setAlpha(int alpha) {
        circlePaint.setAlpha(alpha);
        image.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        circlePaint.setColorFilter(colorFilter);
        image.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        int imageWidth = (int) (bounds.width() * SCALE_FACTOR);
        int imageHeight = (int) (bounds.height() * SCALE_FACTOR);
        int left = (bounds.width() - imageWidth) / 2;
        int top = (bounds.height() - imageHeight) / 2;
        image.setBounds(left + 1, top, left + imageWidth + 1, top + imageHeight);
    }

    void pulse() {
        ValueAnimator pulse = ValueAnimator.ofFloat(0, IMAGE_MOVEMENT_TRANSLATION_X, 0);
        pulse.addUpdateListener(animation -> {
            translationX = (float) animation.getAnimatedValue();
            invalidateSelf();
        });
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setDuration(ANIMATION_DURATION);
        pulse.start();
    }
}
