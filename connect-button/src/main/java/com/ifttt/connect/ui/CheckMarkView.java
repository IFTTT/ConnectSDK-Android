package com.ifttt.connect.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import com.ifttt.connect.R;

import static android.graphics.Color.WHITE;

final class CheckMarkView extends AppCompatImageView {

    public CheckMarkView(Context context) {
        this(context, null);
    }

    public CheckMarkView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckMarkView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int checkMarkSize = getResources().getDimensionPixelSize(R.dimen.ifttt_check_mark_size);
        int circleColor = ContextCompat.getColor(getContext(), R.color.ifttt_semi_transparent_white);
        CheckMarkDrawable drawable = new CheckMarkDrawable(checkMarkSize, circleColor, WHITE);
        setImageDrawable(drawable);

        setAlpha(0f);
    }

    Animator getAnimator(CheckMarkDrawable.AnimatorType type) {
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(this, "alpha", getAlpha(), 1f);
        Animator checkMark = ((CheckMarkDrawable) getDrawable()).getAnimator(type);
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(fadeIn, checkMark);

        return set;
    }

    static CheckMarkView create(FrameLayout parent) {
        CheckMarkView checkMarkView = new CheckMarkView(parent.getContext());
        int width = parent.getResources().getDimensionPixelSize(R.dimen.ifttt_check_mark_width);
        int height = parent.getResources().getDimensionPixelSize(R.dimen.ifttt_check_mark_height);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
        lp.gravity = Gravity.CENTER;
        checkMarkView.setLayoutParams(lp);

        ViewCompat.setElevation(checkMarkView, parent.getResources().getDimension(R.dimen.ifttt_icon_elevation));

        return checkMarkView;
    }
}
