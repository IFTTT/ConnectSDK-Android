package com.ifttt.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import com.ifttt.Connection;
import com.ifttt.R;
import com.ifttt.Service;

/**
 * A static Activity for more information about IFTTT.
 */
public final class IftttAboutActivity extends Activity {

    private static final String EXTRA_CONNECTION = "extra_connection";
    private static final Uri IFTTT = Uri.parse("http://ifttt.com");

    static Intent intent(Context context, Connection connection) {
        return new Intent(context, IftttAboutActivity.class).putExtra(EXTRA_CONNECTION, connection);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_ifttt_about);

        TextView title = findViewById(R.id.ifttt_about_title);
        title.setText(ButtonUiHelper.replaceKeyWithImage(title, title.getText().toString(), "IFTTT",
                ContextCompat.getDrawable(this, R.drawable.ic_ifttt_logo_white)));

        TextView moreButton = findViewById(R.id.ifttt_more_about_button);
        moreButton.setOnClickListener(v -> {
            CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
            intent.launchUrl(this, IFTTT);
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ImageView logoView = findViewById(R.id.ifttt_logo_view);

        Connection connection = getIntent().getParcelableExtra(EXTRA_CONNECTION);
        int primaryColor = connection.getPrimaryService().brandColor;
        int secondaryColor = Color.BLACK;

        // Use the first non-primary service' brand color as the secondary logo color.
        for (Service service : connection.services) {
            if (!service.isPrimary) {
                secondaryColor = service.brandColor;
                break;
            }
        }

        logoView.setImageDrawable(new IconDrawable(primaryColor, secondaryColor));

        // Set up links to terms of use and privacy policy.
        TextView termsAndPrivacy = findViewById(R.id.term_and_privacy);
        termsAndPrivacy.setText(Html.fromHtml(getString(R.string.terms_and_privacy)));
        termsAndPrivacy.setLinkTextColor(Color.WHITE);
        termsAndPrivacy.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
