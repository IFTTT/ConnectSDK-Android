package com.ifttt.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.ifttt.Connection;
import com.ifttt.R;
import com.ifttt.Service;

import static com.ifttt.ui.ButtonApiHelper.redirectToTerms;
import static com.ifttt.ui.ButtonUiHelper.findWorksWithService;

/**
 * A static Activity for more information about IFTTT.
 */
public final class AboutIftttActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_ifttt_about);

        Connection connection = getIntent().getParcelableExtra(EXTRA_CONNECTION);

        Service primaryService = connection.getPrimaryService();
        Service secondaryService = findWorksWithService(connection);

        ImageView primaryServiceIcon = findViewById(R.id.ifttt_primary_service_icon);
        ImageView secondaryServiceIcon = findViewById(R.id.ifttt_secondary_service_icon);
        ImageLoader.get().load(getLifecycle(), primaryService.monochromeIconUrl, primaryServiceIcon::setImageBitmap);
        ImageLoader.get()
                .load(getLifecycle(), secondaryService.monochromeIconUrl, secondaryServiceIcon::setImageBitmap);

        TextView title = findViewById(R.id.ifttt_about_title);
        String aboutTitleString = getString(R.string.ifttt_about_title, secondaryService.name, primaryService.name);
        CharSequence replacedWithIftttLogo = ButtonUiHelper.replaceKeyWithImage(title, aboutTitleString, "IFTTT",
                ContextCompat.getDrawable(this, R.drawable.ic_ifttt_logo_white));
        Typeface bold = ResourcesCompat.getFont(this, R.font.avenir_next_ltpro_bold);
        Spannable highlightServiceNames = new SpannableString(replacedWithIftttLogo);
        int secondaryServiceNameStart = aboutTitleString.indexOf(secondaryService.name);
        int secondaryServiceNameEnd = secondaryServiceNameStart + secondaryService.name.length();
        int primaryServiceNameStart = aboutTitleString.indexOf(primaryService.name);
        int primaryServiceNameEnd = primaryServiceNameStart + primaryService.name.length();
        highlightServiceNames.setSpan(new AvenirTypefaceSpan(bold), secondaryServiceNameStart, secondaryServiceNameEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        highlightServiceNames.setSpan(new AvenirTypefaceSpan(bold), primaryServiceNameStart, primaryServiceNameEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(highlightServiceNames);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set up links to terms of use and privacy policy.
        TextView termsAndPrivacy = findViewById(R.id.term_and_privacy);
        termsAndPrivacy.setText(Html.fromHtml(getString(R.string.terms_and_privacy)));
        termsAndPrivacy.setLinkTextColor(Color.WHITE);
        termsAndPrivacy.setMovementMethod(LinkMovementMethod.getInstance());

        Intent redirectToTermsIntent = redirectToTerms(this);
        if (redirectToTermsIntent != null) {
            termsAndPrivacy.setOnClickListener(v -> {
                startActivity(redirectToTermsIntent);
            });
        }

        View manageConnectionView = findViewById(R.id.ifttt_manage_connection);
        View googlePlayView = findViewById(R.id.google_play_link);

        PackageManager packageManager = getPackageManager();
        if ((connection.status == Connection.Status.enabled || connection.status == Connection.Status.disabled)
                && ButtonUiHelper.isIftttInstalled(packageManager)) {
            googlePlayView.setVisibility(View.GONE);

            Intent manageIntent = ButtonApiHelper.redirectToManage(this, connection.id);
            if (manageIntent != null) {
                manageConnectionView.setVisibility(View.VISIBLE);
                manageConnectionView.setOnClickListener(v -> startActivity(manageIntent));
            }
        } else {
            manageConnectionView.setVisibility(View.GONE);

            Intent storeIntent = ButtonApiHelper.redirectToPlayStore(this);
            if (storeIntent != null) {
                googlePlayView.setVisibility(View.VISIBLE);
                googlePlayView.setOnClickListener(v -> startActivity(storeIntent));
            } else {
                googlePlayView.setVisibility(View.GONE);
            }
        }
    }

    private static final String EXTRA_CONNECTION = "extra_connection";

    public static Intent intent(Context context, Connection connection) {
        return new Intent(context, AboutIftttActivity.class).putExtra(EXTRA_CONNECTION, connection);
    }
}
