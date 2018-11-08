package com.ifttt.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
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
    private static final Uri IFTTT_ABOUT = Uri.parse("http://ifttt.com/about");

    static Intent intent(Context context, Connection connection) {
        return new Intent(context, IftttAboutActivity.class).putExtra(EXTRA_CONNECTION, connection);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_ifttt_about);

        Typeface avenirBold = Typeface.createFromAsset(getAssets(), "avenir_next_ltpro_bold.otf");
        Typeface avenirMedium = Typeface.createFromAsset(getAssets(), "avenir_next_ltpro_medium.otf");

        TextView title = findViewById(R.id.ifttt_about_title);
        title.setTypeface(avenirBold);
        title.setText(ButtonUiHelper.replaceKeyWithImage(title, title.getText().toString(), "IFTTT",
                ContextCompat.getDrawable(this, R.drawable.ifttt_logo_white)));

        TextView description1 = findViewById(R.id.ifttt_about_1);
        description1.setTypeface(avenirMedium);

        TextView description2 = findViewById(R.id.ifttt_about_2);
        description2.setTypeface(avenirMedium);

        TextView description3 = findViewById(R.id.ifttt_about_3);
        description3.setTypeface(avenirMedium);

        TextView moreButton = findViewById(R.id.ifttt_more_about_button);
        moreButton.setTypeface(avenirBold);
        moreButton.setOnClickListener(v -> {
            CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
            intent.launchUrl(this, IFTTT_ABOUT);
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
    }
}
