package com.ifttt.ui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Patterns;
import android.view.ViewTreeObserver;
import android.widget.TextSwitcher;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.TextViewCompat;
import com.ifttt.Connection;
import com.ifttt.R;
import com.ifttt.Service;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import static android.util.TypedValue.COMPLEX_UNIT_PX;

final class ButtonUiHelper {

    private static final String PACKAGE_NAME_IFTTT = "com.ifttt.ifttt";

    @CheckReturnValue
    static int getDarkerColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        hsv[2] = Math.max(0f, Math.min(1, hsv[2] + (hsv[2] < 0.01f ? .15f : -hsv[2] * .15f)));

        float hue = hsv[0];
        float value = hsv[2];
        if (value > 0.8f) {
            hsv[2] = value - 0.01f;
        } else if (hue < 0.8f && hue > 0.6f) {
            hsv[2] = Math.max(0.78f, value + 0.08f);
        } else if (value > 0.25f) {
            hsv[2] = value - 0.01f;
        } else {
            hsv[2] = value + 0.08f;
        }

        return Color.HSVToColor(hsv);
    }

    @CheckReturnValue
    static CharSequence replaceKeyWithImage(TextView textView, String in, String key, final Drawable image) {
        Paint.FontMetrics fontMetrics = textView.getPaint().getFontMetrics();
        float imageHeight = -fontMetrics.ascent;
        float scaleRatio = imageHeight / image.getIntrinsicHeight();
        int width = (int) (image.getIntrinsicWidth() * scaleRatio);
        int height = (int) imageHeight;
        image.setBounds(0, 0, width, height);

        int start = in.indexOf(key);
        CharSequence result = in;
        while (start != -1) {
            int end = start + key.length();
            SpannableString text = new SpannableString(result);
            ImageSpan imageSpan = new ImageSpan(image) {
                @Override
                public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int baseline,
                        int bottom, Paint paint) {
                    canvas.save();
                    canvas.translate(x, baseline - image.getBounds().height());
                    image.draw(canvas);
                    canvas.restore();
                }
            };
            text.setSpan(imageSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            result = text;
            start = in.indexOf(key, end);
        }
        return result;
    }

    @CheckReturnValue
    static Drawable buildButtonBackground(Context context, @ColorInt int color) {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.background_button).mutate();
        DrawableCompat.setTint(DrawableCompat.wrap(drawable), color);
        return drawable;
    }

    @CheckReturnValue
    static boolean isEmailInvalid(@Nullable CharSequence email) {
        if (TextUtils.isEmpty(email)) {
            return true;
        }

        return !Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @CheckReturnValue
    static boolean isIftttInstalled(PackageManager packageManager) {
        try {
            packageManager.getApplicationInfo(PACKAGE_NAME_IFTTT, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @CheckReturnValue
    static Service findWorksWithService(Connection connection) {
        Service otherService = null;
        if (connection.services.size() == 1) {
            // If there is only one service involved.
            otherService = connection.services.get(0);
        } else {
            for (Service service : connection.services) {
                if (!service.isPrimary) {
                    otherService = service;
                    break;
                }
            }
        }

        if (otherService == null) {
            throw new IllegalStateException("There is no primary service for this Connection.");
        }

        return otherService;
    }

    static void adjustTextViewLayout(TextSwitcher switcher, ConnectButtonState state) {
        Resources resources = switcher.getResources();
        int largePadding = resources.getDimensionPixelSize(R.dimen.ifttt_text_padding_horizontal);
        int smallPadding = resources.getDimensionPixelSize(R.dimen.ifttt_text_padding_horizontal_small);

        TextView currentView = (TextView) switcher.getCurrentView();
        TextView nextView = (TextView) switcher.getNextView();

        if (ViewCompat.isLaidOut(currentView)) {
            adjustTextViewPadding(currentView, largePadding, smallPadding, state);
        } else {
            currentView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    currentView.getViewTreeObserver().removeOnPreDrawListener(this);
                    adjustTextViewPadding(currentView, largePadding, smallPadding, state);
                    return false;
                }
            });
        }

        if (ViewCompat.isLaidOut(nextView)) {
            adjustTextViewPadding(nextView, largePadding, smallPadding, state);
        } else {
            nextView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    nextView.getViewTreeObserver().removeOnPreDrawListener(this);
                    adjustTextViewPadding(nextView, largePadding, smallPadding, state);
                    return false;
                }
            });
        }
    }

    static void setTextSwitcherTextColor(TextSwitcher switcher, @ColorInt int color) {
        ((TextView) switcher.getCurrentView()).setTextColor(color);
        ((TextView) switcher.getNextView()).setTextColor(color);
    }

    private static void adjustTextViewPadding(TextView textView, int largePadding, int smallPadding,
            ConnectButtonState buttonState) {
        int maxSize = textView.getResources().getDimensionPixelSize(R.dimen.ifttt_service_name_max_text_size);
        int minSize = textView.getResources().getDimensionPixelSize(R.dimen.ifttt_service_name_min_text_size);

        // Test the TextView using the max size.
        TextPaint textPaint = textView.getPaint();
        textPaint.setTextSize(maxSize);
        float textWidth = StaticLayout.getDesiredWidth(textView.getText(), textPaint);
        float maxWidth = textView.getWidth() - largePadding * 2;
        if (textWidth > maxWidth) {
            // Reduce the right padding if the original text is longer than the available space in this View. This is to
            // keep the text "center" visually.
            if (buttonState == ConnectButtonState.Enabled) {
                textView.setPadding(smallPadding, 0, largePadding, 0);
            } else {
                textView.setPadding(largePadding, 0, smallPadding, 0);
            }

            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(textView, minSize, maxSize, 1, COMPLEX_UNIT_PX);
        } else {
            textView.setPadding(largePadding, 0, largePadding, 0);

            // Disable auto resizing and use the maximum size.
            TextViewCompat.setAutoSizeTextTypeWithDefaults(textView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
            textView.setTextSize(COMPLEX_UNIT_PX, maxSize);
        }
    }

    private ButtonUiHelper() {
        throw new AssertionError("No instance.");
    }
}
