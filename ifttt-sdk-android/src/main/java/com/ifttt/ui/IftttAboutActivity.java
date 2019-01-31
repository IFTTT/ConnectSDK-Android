package com.ifttt.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.ifttt.R;

/**
 * A static Activity for more information about IFTTT.
 */
public final class IftttAboutActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_ifttt_about);

        TextView title = findViewById(R.id.ifttt_about_title);
        title.setText(ButtonUiHelper.replaceKeyWithImage(title, title.getText().toString(), "IFTTT",
                ContextCompat.getDrawable(this, R.drawable.ic_ifttt_logo_white)));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set up links to terms of use and privacy policy.
        TextView termsAndPrivacy = findViewById(R.id.term_and_privacy);
        termsAndPrivacy.setText(Html.fromHtml(getString(R.string.terms_and_privacy)));
        termsAndPrivacy.setLinkTextColor(Color.WHITE);
        termsAndPrivacy.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
