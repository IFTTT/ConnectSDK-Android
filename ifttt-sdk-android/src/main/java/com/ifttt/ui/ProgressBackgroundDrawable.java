package com.ifttt.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.graphics.drawable.shapes.RoundRectShape;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import javax.annotation.Nullable;

final class ProgressBackgroundDrawable extends Drawable {

    private final Path progressPath = new Path();

    private final ShapeDrawable drawable = new ShapeDrawable();
    private final ShapeDrawable progressDrawable = new ShapeDrawable();

    /**
     * Set up the drawable as a progress bar.
     *
     * @param primaryColor Primary color of the drawable.
     * @param progressColor Progress bar color of the drawable.
     */
    void setColor(@ColorInt int primaryColor, @ColorInt int progressColor) {
        drawable.getPaint().setColor(primaryColor);
        progressDrawable.getPaint().setColor(progressColor);
        invalidateSelf();
    }

    void setProgress(@FloatRange(from = 0.0f, to = 1.0f) float progress) {

        progressPath.reset();

        Rect bounds = drawable.getBounds();
        float radius = bounds.height() / 2f;
        progressPath.moveTo(radius, bounds.bottom);
        float leftArcEnds = radius / (float) (bounds.width());
        float rightArcStarts = (bounds.right - radius) / bounds.width();
        if (progress >= leftArcEnds && progress < rightArcStarts) {
            // Can draw the full left arc but not right arc.
            progressPath.arcTo(new RectF(0f, 0f, radius * 2, bounds.height()), 90, 180);
            float progressWithoutArc = progress - leftArcEnds;
            progressPath.rLineTo(progressWithoutArc * bounds.width(), 0);
            progressPath.rLineTo(0, bounds.height());
            progressPath.rLineTo(-progressWithoutArc * bounds.width(), 0);
        } else if (progress < leftArcEnds) {
            // Can only draw partial left arc.
            Path rectPath = new Path();
            rectPath.lineTo(progress * bounds.width(), 0);
            rectPath.rLineTo(0, bounds.height());
            rectPath.rLineTo(-progress * bounds.width(), 0);
            rectPath.close();

            Path arcPath = new Path();
            arcPath.arcTo(new RectF(0f, 0f, radius * 2, bounds.height()), 90, 180);
            arcPath.close();

            arcPath.op(rectPath, Path.Op.INTERSECT);
            progressPath.addPath(arcPath);
        } else if (progress > rightArcStarts) {
            // Can draw right arc.
            progressPath.arcTo(new RectF(0f, 0f, radius * 2, bounds.height()), 90, 180);
            progressPath.rLineTo(bounds.width() - radius * 2, 0);
            progressPath.rLineTo(0, bounds.bottom);
            progressPath.close();

            float rightArcProgress = progress - rightArcStarts;
            Path rectPath = new Path();
            rectPath.moveTo(bounds.width() - radius, 0);
            rectPath.rLineTo(rightArcProgress * bounds.width(), 0);
            rectPath.rLineTo(0, bounds.height());
            rectPath.rLineTo(-rightArcProgress * bounds.width(), 0);
            rectPath.close();

            Path arcPath = new Path();
            arcPath.moveTo(bounds.width() - radius, 0);
            arcPath.arcTo(new RectF(bounds.width() - radius * 2, 0, bounds.width(), bounds.height()), 270, 180);
            arcPath.close();
            arcPath.op(rectPath, Path.Op.INTERSECT);

            progressPath.addPath(arcPath);
        }

        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        drawable.draw(canvas);
        progressDrawable.draw(canvas);
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

        if (!progressDrawable.getBounds().equals(bounds)) {
            progressDrawable.setShape(new PathShape(progressPath, bounds.width(), bounds.height()));
            progressDrawable.setBounds(bounds);
        }
    }
}
