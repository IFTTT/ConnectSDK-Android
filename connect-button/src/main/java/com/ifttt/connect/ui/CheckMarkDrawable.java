package com.ifttt.connect.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import javax.annotation.Nullable;

final class CheckMarkDrawable extends Drawable {

    enum AnimatorType {
        COMPLETE, ENABLE
    }

    private static final Interpolator INTERPOLATOR = input -> (float) (Math.pow((input - 1), 5) + 1);
    private static final long ANIM_CIRCLE_DURATION = 800L;
    private static final long ANIM_CHECK_MARK_DURATION = 600L;
    private static final long ANIM_SCALE_DURATION = 400L;
    private static final long ANIM_PATH_SCALE_DOWN_START_DELAY = 300L;
    private static final float CHECK_MARK_SCALE = 0.8f;

    private final Paint circlePaint;
    private final Paint dotPaint;
    private final Paint checkMarkPaint;
    private final int checkMarkSize;

    // Animation values
    private final float[] dotPos = new float[2];
    private final Path dotPath = new Path();
    private final Path checkMarkPath = new Path();

    private PathMeasure circlePathMeasure;
    private PathMeasure checkMarkPathMeasure;
    private boolean drawCheckMark = false;
    private float scale = 0f;

    CheckMarkDrawable(int checkMarkSize, int circleColor, int checkMarkColor) {
        this.checkMarkSize = checkMarkSize;
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(circleColor);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(5f);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(checkMarkColor);

        checkMarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkMarkPaint.setColor(checkMarkColor);
        checkMarkPaint.setStyle(Paint.Style.STROKE);
        checkMarkPaint.setStrokeWidth(10f);
        checkMarkPaint.setStrokeCap(Paint.Cap.ROUND);
        checkMarkPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    public void draw(Canvas canvas) {
        int count = canvas.save();
        int radius = checkMarkSize / 2;
        int cx = getBounds().width() / 2;
        int cy = getBounds().height() / 2;
        canvas.scale(scale, scale, cx, cy);

        canvas.drawCircle(cx, cy, radius, circlePaint);

        // Scale down the check mark without changing the path.
        canvas.scale(CHECK_MARK_SCALE, CHECK_MARK_SCALE, cx, cy);
        if (drawCheckMark) {
            canvas.drawPath(checkMarkPath, checkMarkPaint);
        } else {
            if (dotPos[0] == 0 && dotPos[1] == 0) {
                dotPos[0] = cx;
                dotPos[1] = cy;
            }
            canvas.drawCircle(dotPos[0], dotPos[1], 5f, dotPaint);
        }

        canvas.restoreToCount(count);
    }

    @Override
    public void setAlpha(int alpha) {
        circlePaint.setAlpha(alpha);
        dotPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        circlePaint.setColorFilter(colorFilter);
        dotPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        checkMarkPaint.setPathEffect(null);
        dotPath.reset();
        checkMarkPath.reset();
        scale = 0f;

        int radius = Math.min(getBounds().width(), getBounds().height()) / 2 - 20;
        int cx = getBounds().width() / 2;
        int cy = getBounds().height() / 2;
        dotPath.moveTo(cx, cy);
        dotPath.quadTo(0, getBounds().bottom, 10, cy);

        float endX = (float) (cx - 0.25f * radius - Math.sqrt(2) * radius * 0.25f);
        float endY = (float) (cy + 0.4f * radius - Math.sqrt(2) * radius * 0.25f);
        dotPath.quadTo(0f, 0f, endX, endY);
        circlePathMeasure = new PathMeasure(dotPath, false);

        float[] whereDotStops = new float[2];
        circlePathMeasure.getPosTan(circlePathMeasure.getLength(), whereDotStops, null);
        checkMarkPath.moveTo(whereDotStops[0], whereDotStops[1]);
        checkMarkPath.lineTo(cx - 0.25f * radius, cy + 0.4f * radius);

        double degree = 90 - Math.toDegrees(Math.atan(0.6d / 0.75d));
        float atan = (float) (Math.tan(Math.toRadians(90 - degree)) * 0.45);
        checkMarkPath.lineTo(cx + 0.45f * radius, cy - atan * radius);
        checkMarkPathMeasure = new PathMeasure(checkMarkPath, false);
    }

    Animator getAnimator(AnimatorType type) {
        ValueAnimator pathTracing = ValueAnimator.ofFloat(0f, 1f);
        pathTracing.setInterpolator(new LinearInterpolator());
        pathTracing.addUpdateListener(animation -> {
            circlePathMeasure.getPosTan(animation.getAnimatedFraction() * circlePathMeasure.getLength(), dotPos, null);
            invalidateSelf();
        });

        pathTracing.setDuration(ANIM_CIRCLE_DURATION);

        ValueAnimator checkMark = ValueAnimator.ofFloat(1f, 0f).setDuration(ANIM_CHECK_MARK_DURATION);
        checkMark.setInterpolator(INTERPOLATOR);
        checkMark.addUpdateListener(animation -> {
            float length = checkMarkPathMeasure.getLength();
            float value = (float) animation.getAnimatedValue() * length;
            checkMarkPaint.setPathEffect(new DashPathEffect(new float[] { length, length }, value));
            invalidateSelf();
        });
        checkMark.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                drawCheckMark = true;
            }
        });

        ValueAnimator.AnimatorUpdateListener updateListener = animation -> {
            scale = (float) animation.getAnimatedValue();
            invalidateSelf();
        };
        ValueAnimator scaleUp = ValueAnimator.ofFloat(0f, 1f).setDuration(ANIM_SCALE_DURATION);
        scaleUp.setInterpolator(INTERPOLATOR);
        scaleUp.addUpdateListener(updateListener);

        AnimatorSet set = new AnimatorSet();
        if (type == AnimatorType.COMPLETE) {
            ValueAnimator scaleDown = ValueAnimator.ofFloat(1f, 0f).setDuration(ANIM_SCALE_DURATION);
            scaleDown.addUpdateListener(updateListener);
            scaleDown.setStartDelay(ANIM_PATH_SCALE_DOWN_START_DELAY);
            set.playSequentially(pathTracing, checkMark, scaleDown);
        } else if (type == AnimatorType.ENABLE) {
            set.playSequentially(pathTracing, checkMark);
        } else {
            throw new IllegalStateException("Unsupported animator type: " + type);
        }

        set.playTogether(scaleUp, pathTracing);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                drawCheckMark = false;
            }
        });

        return set;
    }
}
