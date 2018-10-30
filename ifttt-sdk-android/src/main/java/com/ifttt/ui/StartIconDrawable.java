package com.ifttt.ui;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import com.ifttt.R;
import javax.annotation.Nullable;

import static android.graphics.Color.BLACK;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

final class StartIconDrawable extends Drawable {

    private static final ArgbEvaluator EVALUATOR = new ArgbEvaluator();

    private final ShapeDrawable background = new ShapeDrawable();
    private final Drawable serviceIcon;
    private final Drawable startIcon;
    private final int iconSize;
    private final int initialBackgroundSize;
    private final int startIconWidth;
    private final int startIconHeight;

    StartIconDrawable(Context context, Drawable serviceIcon, int iconSize, int initialBackgroundSize) {
        this.serviceIcon = serviceIcon;
        this.startIcon = ContextCompat.getDrawable(context, R.drawable.ic_start_arrow);
        startIconWidth = context.getResources().getDimensionPixelSize(R.dimen.ifttt_start_image_width);
        startIconHeight = context.getResources().getDimensionPixelSize(R.dimen.ifttt_start_image_height);
        this.iconSize = iconSize;
        this.initialBackgroundSize = initialBackgroundSize;

        this.startIcon.setAlpha(0);
        this.serviceIcon.setAlpha(255);
    }

    @Override
    public void draw(Canvas canvas) {
        background.draw(canvas);
        serviceIcon.draw(canvas);
        startIcon.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        // No-op, the alpha value of each of the internal Drawables will be controlled by animations only.
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        background.setColorFilter(colorFilter);
        serviceIcon.setColorFilter(colorFilter);
        startIcon.setColorFilter(colorFilter);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        int cx = bounds.width() / 2;
        int cy = bounds.height() / 2;
        serviceIcon.setBounds(cx - iconSize / 2, cy - iconSize / 2, cx + iconSize / 2, cy + iconSize / 2);
        startIcon.setBounds(cx - startIconWidth / 2, cy - startIconHeight / 2, cx + startIconWidth / 2,
                cy + startIconHeight / 2);

        background.setBounds(cx - initialBackgroundSize / 2, cy - initialBackgroundSize / 2,
                cx + initialBackgroundSize / 2, cy + initialBackgroundSize / 2);

        float radius = initialBackgroundSize / 2f;
        background.setShape(
                new RoundRectShape(new float[] { radius, radius, radius, radius, radius, radius, radius, radius }, null,
                        null));
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @RequiresApi(api = LOLLIPOP)
    @Override
    public void getOutline(Outline outline) {
        background.getOutline(outline);
    }

    void reset() {
        int width = getBounds().width();
        int height = getBounds().height();
        int bgWidthDiff = Math.max(0, width - initialBackgroundSize);
        int bgHeightDiff = Math.max(0, height - initialBackgroundSize);
        float radius = initialBackgroundSize / 2f;
        background.setShape(
                new RoundRectShape(new float[] { radius, radius, radius, radius, radius, radius, radius, radius }, null,
                        null));
        background.setBounds(bgWidthDiff / 2, bgHeightDiff / 2, width - bgWidthDiff / 2, height - bgHeightDiff / 2);

        serviceIcon.setAlpha(255);
        startIcon.setAlpha(0);
        invalidateSelf();
    }

    void setBackgroundColor(@ColorInt int color) {
        background.getPaint().setColor(color);
        invalidateSelf();
    }

    Animator getAnimator() {
        int width = getBounds().width();
        int height = getBounds().height();
        int backgroundColor = background.getPaint().getColor();
        ValueAnimator iconMorphing = ValueAnimator.ofFloat(initialBackgroundSize, Math.min(width, height));
        iconMorphing.addUpdateListener(animation -> {
            float progress = animation.getAnimatedFraction();
            float radius = (1 - progress) * height / 2f;
            int rounded = height / 2;
            background.setShape(new RoundRectShape(new float[] {
                    radius, radius, rounded, rounded, rounded, rounded, radius, radius
            }, null, null));

            Integer color = (Integer) EVALUATOR.evaluate(progress, backgroundColor, BLACK);
            background.getPaint().setColor(color);

            float animatedSize = (float) animation.getAnimatedValue();
            int bgWidthDiff = (int) Math.max(0, width - animatedSize);
            int bgHeightDiff = (int) Math.max(0, height - animatedSize);
            background.setBounds(bgWidthDiff / 2, bgHeightDiff / 2, width - bgWidthDiff / 2, height - bgHeightDiff / 2);

            serviceIcon.setAlpha((int) (255 * (1 - progress)));
            startIcon.setAlpha((int) (255 * progress));
            invalidateSelf();
        });

        return iconMorphing;
    }
}