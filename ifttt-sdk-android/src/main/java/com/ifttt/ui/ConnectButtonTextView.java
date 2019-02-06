package com.ifttt.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.StaticLayout;
import android.util.AttributeSet;
import android.widget.TextSwitcher;
import android.widget.TextView;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import com.ifttt.R;

/**
 * Custom {@link TextSwitcher} that adjusts padding for based on the text length, as well as managing the dots
 * animation.
 */
final class ConnectButtonTextView extends TextSwitcher {

    private final AnimatedVectorDrawableCompat dotsAnimator;

    public ConnectButtonTextView(Context context) {
        this(context, null);
    }

    public ConnectButtonTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        int width = context.getResources().getDimensionPixelSize(R.dimen.ifttt_dots_width);
        int height = context.getResources().getDimensionPixelSize(R.dimen.ifttt_dots_height);
        dotsAnimator = AnimatedVectorDrawableCompat.create(context, R.drawable.ifttt_continue_dots);
        dotsAnimator.setBounds(0, 0, width, height);
        dotsAnimator.registerAnimationCallback(new AnimatedVectorDrawableCompat.AnimationCallback() {
            @Override
            public void onAnimationEnd(Drawable drawable) {
                post(dotsAnimator::start);
            }
        });
    }

    /**
     * Add the dots animations to the current view of this TextSwitcher.
     *
     * @param showAnimation true if the animation should be added, false otherwise.
     */
    void showDotsAnimation(boolean showAnimation) {
        TextView currentView = (TextView) getCurrentView();
        TextView nextView = (TextView) getNextView();
        if (showAnimation) {
            currentView.setCompoundDrawables(null, null, dotsAnimator, null);
            nextView.setCompoundDrawables(null, null, dotsAnimator, null);
            dotsAnimator.start();
        } else {
            currentView.setCompoundDrawables(null, null, null, null);
            nextView.setCompoundDrawables(null, null, null, null);
        }
    }

    @Override
    public void setText(CharSequence text) {
        super.setText(text);

        int paddingHorizontal = getResources().getDimensionPixelSize(R.dimen.ifttt_text_padding_horizontal);
        int paddingSmall = getResources().getDimensionPixelSize(R.dimen.ifttt_text_padding_horizontal_small);

        TextView currentView = (TextView) getCurrentView();
        float textWidth = StaticLayout.getDesiredWidth(text, currentView.getPaint());
        float maxWidth = getWidth() - paddingHorizontal * 2;
        // Reduce the right padding if the original text is longer than the available space in this View. This is to
        // keep the text "center" visually.
        if (textWidth > maxWidth) {
            setPadding(paddingHorizontal, 0, paddingSmall, 0);
        } else {
            setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        }
    }
}
