package com.ifttt.ui;

import android.content.Context;
import android.text.StaticLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.ifttt.R;
import javax.annotation.Nullable;

final class ServiceNameTextView extends LinearLayout {

    private final TextView serviceText;

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
    }

    CharSequence getText() {
        return serviceText.getText();
    }

    void setText(@Nullable CharSequence text) {
        serviceText.setText(text);

        if (text == null) {
            return;
        }

        serviceText.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                serviceText.getViewTreeObserver().removeOnPreDrawListener(this);
                adjustServiceText(text);
                return false;
            }
        });
    }

    private void adjustServiceText(CharSequence serviceName) {
        int paddingHorizontal = getResources().getDimensionPixelSize(R.dimen.ifttt_text_padding_horizontal);
        int paddingSmall = getResources().getDimensionPixelSize(R.dimen.ifttt_text_padding_horizontal_small);

        float textWidth = StaticLayout.getDesiredWidth(serviceName, serviceText.getPaint());
        // The maximum width allowed for a service name is half of this View's width.
        float maxWidth = getWidth() - paddingHorizontal * 2;
        if (textWidth > maxWidth) {
            setPadding(paddingHorizontal, 0, paddingSmall, 0);
        } else {
            // Magic numbers: if the amount of px need to render the text is more than 3 times of the padding, apply
            // the padding, otherwise do not. This is to maintain a feeling of the text being in the center while not
            // being covered by the service icon.
            setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        }
    }
}
