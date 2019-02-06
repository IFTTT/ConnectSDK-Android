package com.ifttt.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.TextSwitcher;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.ifttt.R;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import static com.ifttt.ui.ButtonUiHelper.TextTransitionType.Change;

final class ButtonUiHelper {

    enum TextTransitionType {
        Appear, Change
    }

    interface OnTextSwitchedListener {
        void onSwitched();
    }

    private static final FastOutSlowInInterpolator INTERPOLATOR = new FastOutSlowInInterpolator();
    private static final long ANIM_DURATION = 300L;

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
    static Animator getTextTransitionAnimator(View view, TextTransitionType type, OnTextSwitchedListener listener) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        ObjectAnimator slideIn = ObjectAnimator.ofFloat(view, "translationY", -50f, 0f);
        ObjectAnimator slideOut = ObjectAnimator.ofFloat(view, "translationY", 0f, 50f);

        AnimatorSet set = new AnimatorSet();
        if (type == TextTransitionType.Appear) {
            set.play(fadeIn).with(slideIn);
        } else if (type == Change) {
            fadeIn.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    listener.onSwitched();
                }
            });

            set.play(fadeOut).with(slideOut);
            set.play(fadeIn).with(slideIn);
            set.play(fadeOut).before(fadeIn);
        }

        set.setDuration(ANIM_DURATION);
        set.setInterpolator(INTERPOLATOR);

        return set;
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

    static void setTextSwitcherText(TextSwitcher textSwitcher, CharSequence text) {
        TextView currentText = (TextView) textSwitcher.getCurrentView();
        if (currentText.getText().equals(text)) {
            return;
        }

        textSwitcher.setText(text);
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

    private ButtonUiHelper() {
        throw new AssertionError("No instance.");
    }
}
