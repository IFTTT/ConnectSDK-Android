package com.ifttt.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import javax.annotation.Nullable;

/**
 * Custom {@link Drawable} that renders the Logo in {@link IftttAboutActivity}.
 */
final class IconDrawable extends Drawable {

    private final Paint primaryColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secondaryColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    IconDrawable(@ColorInt int primaryColor, @ColorInt int secondaryColor) {
        primaryColorPaint.setColor(primaryColor);
        secondaryColorPaint.setColor(secondaryColor);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        canvas.drawRect(new RectF(0f, 0f, bounds.right, bounds.height() * 2 / 3f), primaryColorPaint);
        canvas.drawRect(new RectF(0, bounds.height() * 2 / 3f, bounds.right, bounds.bottom), secondaryColorPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        primaryColorPaint.setAlpha(alpha);
        secondaryColorPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        primaryColorPaint.setColorFilter(colorFilter);
        secondaryColorPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
