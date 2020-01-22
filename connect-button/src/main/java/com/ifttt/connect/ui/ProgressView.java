package com.ifttt.connect.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.ifttt.connect.R;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

final class ProgressView extends FrameLayout {

    private final TextSwitcher textSwitcher;

    public ProgressView(@NonNull Context context) {
        this(context, null);
    }

    public ProgressView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.view_ifttt_progress, this);

        textSwitcher = findViewById(R.id.ifttt_progress_text);

        if (SDK_INT >= KITKAT) {
            // Only use ProgressBackgroundDrawable on Android 19 or above.
            ProgressBackgroundKitKat progressRootBg = new ProgressBackgroundKitKat();
            setBackground(progressRootBg);
        } else {
            ProgressBackgroundJellyBean progressRootBg = new ProgressBackgroundJellyBean();
            setBackground(progressRootBg);
        }

        setAlpha(0f);
    }

    void setProgressColor(@ColorInt int primaryColor, @ColorInt int progressColor) {
        ((ProgressBackground) getBackground()).setColor(primaryColor, progressColor);
    }

    Animator progress(@FloatRange(from = 0.0f, to = 1.0f) float progressFrom,
            @FloatRange(from = 0.0f, to = 1.0f) float progressTo, CharSequence text, long duration) {
        CharSequence currentText = ((TextView) textSwitcher.getCurrentView()).getText();
        if (!currentText.equals(text)) {
            textSwitcher.setText(text);
        }

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(this, "alpha", getAlpha(), 1f);
        ValueAnimator progress = ValueAnimator.ofFloat(progressFrom, progressTo);
        progress.setDuration(duration);
        progress.addUpdateListener(
                animation -> ((ProgressBackground) getBackground()).setProgress((Float) animation.getAnimatedValue()));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeIn, progress);
        return set;
    }

    void setProgressText(@Nullable CharSequence text) {
        textSwitcher.setText(text);
    }

    static ProgressView create(ViewGroup parent, @ColorInt int primaryColor, @ColorInt int progressColor) {
        ProgressView progressView = new ProgressView(parent.getContext());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        progressView.setLayoutParams(lp);
        parent.addView(progressView);

        ViewCompat.setElevation(progressView, parent.getResources().getDimension(R.dimen.ifttt_icon_elevation));

        progressView.setProgressColor(primaryColor, progressColor);
        return progressView;
    }
}
