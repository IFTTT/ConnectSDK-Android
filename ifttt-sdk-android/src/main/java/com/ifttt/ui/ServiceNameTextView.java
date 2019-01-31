package com.ifttt.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.StaticLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import com.ifttt.R;
import javax.annotation.Nullable;

final class ServiceNameTextView extends LinearLayout {

    private final TextView serviceText;
    private final ImageView dotsView;

    public ServiceNameTextView(Context context) {
        this(context, null);
    }

    public ServiceNameTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ServiceNameTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.view_service_name, this);
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER);

        serviceText = findViewById(R.id.ifttt_service_name);
        dotsView = findViewById(R.id.ifttt_continue_dots);

        AnimatedVectorDrawableCompat dots =
                AnimatedVectorDrawableCompat.create(context, R.drawable.ifttt_continue_dots);
        dotsView.setImageDrawable(dots);
        dots.registerAnimationCallback(new AnimatedVectorDrawableCompat.AnimationCallback() {
            @Override
            public void onAnimationEnd(Drawable drawable) {
                post(dots::start);
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((AnimatedVectorDrawableCompat) dotsView.getDrawable()).stop();
    }

    CharSequence getText() {
        return serviceText.getText();
    }

    void setText(@Nullable CharSequence text) {
        setText(text, false);
    }

    void setText(@Nullable CharSequence text, boolean showAnimation) {
        serviceText.setText(text);

        if (text == null) {
            return;
        }

        int paddingHorizontal = getResources().getDimensionPixelSize(R.dimen.ifttt_text_padding_horizontal);
        int paddingSmall = getResources().getDimensionPixelSize(R.dimen.ifttt_text_padding_horizontal_small);
        int dotsSize = getResources().getDimensionPixelSize(R.dimen.ifttt_dots_size);

        float textWidth = StaticLayout.getDesiredWidth(text, serviceText.getPaint());
        // The maximum width allowed for a service name is half of this View's width.
        float maxWidth = getWidth() - paddingHorizontal * 2 - (showAnimation ? dotsSize : 0);
        if (textWidth > maxWidth) {
            setPadding(paddingHorizontal, 0, paddingSmall, 0);
        } else {
            // Magic numbers: if the amount of px need to render the text is more than 3 times of the padding, apply
            // the padding, otherwise do not. This is to maintain a feeling of the text being in the center while not
            // being covered by the service icon.
            setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        }

        if (showAnimation) {
            dotsView.setVisibility(View.VISIBLE);
            ((AnimatedVectorDrawableCompat) dotsView.getDrawable()).start();
        } else {
            dotsView.setVisibility(View.GONE);
            ((AnimatedVectorDrawableCompat) dotsView.getDrawable()).stop();
        }
    }
}
