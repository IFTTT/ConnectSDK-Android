package com.ifttt.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.StaticLayout;
import android.util.AttributeSet;
import android.widget.TextSwitcher;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.ifttt.R;

/**
 * Custom {@link TextSwitcher} that adjusts padding for based on the text length, as well as managing the dots
 * animation.
 */
final class ConnectButtonTextView extends TextSwitcher {

    public ConnectButtonTextView(Context context) {
        this(context, null);
    }

    public ConnectButtonTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void showContinueAnimation(int color) {
        TextView currentView = (TextView) getCurrentView();

        int size = getResources().getDimensionPixelSize(R.dimen.ifttt_continue_icon_size);
        Drawable image = ContextCompat.getDrawable(getContext(), R.drawable.ic_continue_triangle);
        DrawableCompat.setTint(image, color);
        ContinueDrawable continueDrawable = new ContinueDrawable(image, ButtonUiHelper.getDarkerColor(color));
        continueDrawable.setBounds(0, 0, size, size);
        currentView.setCompoundDrawables(null, null, continueDrawable, null);
        continueDrawable.pulse();
        applyPadding();
    }

    void hideContinueAnimation() {
        TextView currentView = (TextView) getCurrentView();
        TextView nextView = (TextView) getNextView();
        currentView.setCompoundDrawables(null, null, null, null);
        nextView.setCompoundDrawables(null, null, null, null);
        applyPadding();
    }

    @Override
    public void setText(CharSequence text) {
        super.setText(text);
        applyPadding();
    }

    private void applyPadding() {
        int paddingHorizontal = getResources().getDimensionPixelSize(R.dimen.ifttt_text_padding_horizontal);
        int paddingSmall = getResources().getDimensionPixelSize(R.dimen.ifttt_text_padding_horizontal_small);

        TextView currentView = (TextView) getCurrentView();
        float textWidth = StaticLayout.getDesiredWidth(currentView.getText(), currentView.getPaint());
        float maxWidth = getWidth() - paddingHorizontal * 2;
        if (currentView.getCompoundDrawables()[2] != null) {
            // Reduce horizontal padding when the animation is playing, this is to give more space to the text so that
            // it can show larger text size.
            setPadding(paddingSmall, 0, paddingSmall / 2, 0);
        } else if (textWidth > maxWidth) {
            // Reduce the right padding if the original text is longer than the available space in this View. This is to
            // keep the text "center" visually.
            setPadding(paddingHorizontal, 0, paddingSmall, 0);
        } else {
            setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        }
    }
}
