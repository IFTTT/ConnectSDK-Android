package com.ifttt.connect.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import com.ifttt.connect.R;
import javax.annotation.Nullable;

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
    private final boolean onDarkBackground;

    private final ShapeDrawable borderDrawable = new ShapeDrawable();

    StartIconDrawable(Context context, Drawable serviceIcon, int iconSize, int initialBackgroundSize,
            boolean onDarkBackground) {
        this.serviceIcon = serviceIcon;
        this.startIcon = ContextCompat.getDrawable(context, R.drawable.ic_start_arrow);
        startIconWidth = context.getResources().getDimensionPixelSize(R.dimen.ifttt_start_image_width);
        startIconHeight = context.getResources().getDimensionPixelSize(R.dimen.ifttt_start_image_height);
        this.iconSize = iconSize;
        this.initialBackgroundSize = initialBackgroundSize;
        this.onDarkBackground = onDarkBackground;
        if (onDarkBackground) {
            startIcon.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        } else {
            startIcon.setColorFilter(null);
        }

        this.startIcon.setAlpha(0);
        this.serviceIcon.setAlpha(255);

        background.getPaint().setColor(Color.TRANSPARENT);
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

        borderDrawable.setShape(background.getShape());
        borderDrawable.setBounds(background.getBounds());
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @RequiresApi(api = LOLLIPOP)
    @Override
    public void getOutline(Outline outline) {
        if (background.getPaint().getColor() == Color.TRANSPARENT) {
            // Do not set outline if the background's color is transparent.
            return;
        }

        outline.setOval(background.getBounds());
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
        if (onDarkBackground) {
            background.getPaint().setColor(ButtonUiHelper.getDarkerColor(color));
        } else {
            background.getPaint().setColor(color);
        }

        invalidateSelf();
    }

    Animator getMorphAnimator() {
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

            Integer color = (Integer) EVALUATOR.evaluate(progress, backgroundColor,
                    ButtonUiHelper.getDarkerColor(backgroundColor));
            background.getPaint().setColor(color);
            // Remove any press state color filter.
            background.getPaint().setColorFilter(null);

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

    private Animator getPressedAnimator(boolean pressed) {
        int originalColor = background.getPaint().getColor();
        int darkerColor = ButtonUiHelper.getDarkerColor(originalColor);
        ValueAnimator pressAnimator;
        if (pressed) {
            pressAnimator = ValueAnimator.ofFloat(0f, 1f);
        } else {
            pressAnimator = ValueAnimator.ofFloat(1f, 0f);
        }
        pressAnimator.addUpdateListener(animation -> {
            background.getPaint()
                    .setColorFilter(new PorterDuffColorFilter(
                            (Integer) EVALUATOR.evaluate((Float) animation.getAnimatedValue(), originalColor,
                                    darkerColor), PorterDuff.Mode.SRC_IN));
            invalidateSelf();
        });
        pressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!pressed) {
                    // If not pressed, at the end of the animation, remove the ColorFilter.
                    background.setColorFilter(null);
                }
            }
        });
        return pressAnimator;
    }

    @VisibleForTesting
    static boolean isDarkColor(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        return hsv[2] < 0.25f;
    }

    /**
     * Set a {@link View.OnTouchListener} on the ImageView that uses StartIconDrawable as background. We will be using
     * a custom animation to render press state.
     *
     * @param view The ImageView that has a StartIconDrawable as background. This method is no-op if this is not the
     * case.
     */
    @SuppressLint("ClickableViewAccessibility")
    static void setPressListener(ImageView view) {
        view.setOnTouchListener(new View.OnTouchListener() {

            @Nullable private Animator ongoingAnimator;

            private float downX;
            private float downY;

            private int getDistance(float x, float y) {
                return (int) Math.sqrt(Math.pow(x - downX, 2) + Math.pow(y - downY, 2));
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!(view.getBackground() instanceof StartIconDrawable)) {
                    // No-op if the ImageView doesn't host a StartIconDrawable.
                    return false;
                }

                StartIconDrawable drawable = (StartIconDrawable) view.getBackground();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ongoingAnimator = drawable.getPressedAnimator(true);
                        ongoingAnimator.start();
                        downX = event.getX();
                        downY = event.getY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        // Fall through
                    case MotionEvent.ACTION_CANCEL:
                        if (ongoingAnimator == null) {
                            return true;
                        }

                        ongoingAnimator.cancel();
                        Animator animator = drawable.getPressedAnimator(false);
                        animator.start();
                        ongoingAnimator = null;

                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            int slop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
                            if (getDistance(event.getX(), event.getY()) < slop) {
                                // Immediately end the animation.
                                animator.end();
                                v.performClick();
                            }
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
    }
}
