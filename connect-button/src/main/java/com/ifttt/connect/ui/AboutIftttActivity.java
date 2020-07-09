package com.ifttt.connect.ui;

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
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.ifttt.connect.R;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.Service;

import static com.ifttt.connect.ui.ButtonApiHelper.redirectToConnection;
import static com.ifttt.connect.ui.ButtonApiHelper.redirectToService;
import static com.ifttt.connect.ui.ButtonApiHelper.redirectToTerms;
import static com.ifttt.connect.ui.ButtonUiHelper.findWorksWithService;

/**
 * A static Activity for more information about IFTTT.
 */
public final class AboutIftttActivity extends AppCompatActivity {

    private AnalyticsManager analyticsManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.view_ifttt_about);

        Connection connection = getIntent().getParcelableExtra(EXTRA_CONNECTION);
        analyticsManager = AnalyticsManager.getInstance(this.getApplicationContext());

        Service primaryService = connection.getPrimaryService();
        Service secondaryService = findWorksWithService(connection);

        ImageView primaryServiceIcon = findViewById(R.id.ifttt_primary_service_icon);
        ImageView secondaryServiceIcon = findViewById(R.id.ifttt_secondary_service_icon);
        ImageLoader.get().load(getLifecycle(), primaryService.monochromeIconUrl, primaryServiceIcon::setImageBitmap);
        ImageLoader.get()
                .load(getLifecycle(), secondaryService.monochromeIconUrl, secondaryServiceIcon::setImageBitmap);
        primaryServiceIcon.setOnClickListener(v -> {
            Intent intent = redirectToService(this, primaryService.id);
            if (intent == null) {
                return;
            }

            analyticsManager.trackUiClick(AnalyticsObject.fromService(primaryService.id), AnalyticsLocation.fromConnectButtonWithId(connection.id));
            startActivity(intent);
        });
        secondaryServiceIcon.setOnClickListener(v -> {
            Intent intent = redirectToService(this, secondaryService.id);
            if (intent == null) {
                return;
            }

            analyticsManager.trackUiClick(AnalyticsObject.fromService(secondaryService.id), AnalyticsLocation.fromConnectButtonWithId(connection.id));
            startActivity(intent);
        });

        TextView title = findViewById(R.id.ifttt_about_title);
        String aboutTitleString = getString(R.string.about_ifttt_connects_x_to_y, secondaryService.name, primaryService.name);
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
        title.setOnClickListener(v -> {
            Intent intent = redirectToConnection(this, connection.id);
            if (intent == null) {
                return;
            }

            analyticsManager.trackUiClick(AnalyticsObject.CONNECTION_NAME, AnalyticsLocation.fromConnectButtonWithId(connection.id));
            startActivity(intent);
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set up links to terms of use and privacy policy.
        TextView termsAndPrivacy = findViewById(R.id.term_and_privacy);

        String content = getString(R.string.about_ifttt_privacy_and_terms);
        String termPrivacyAndTerms = getString(R.string.term_privacy_and_terms);

        int termPrivacyAndTermsIndex = content.indexOf(termPrivacyAndTerms);

        SpannableString spanContent = new SpannableString(content);
        spanContent.setSpan(new UnderlineSpan(), termPrivacyAndTermsIndex, termPrivacyAndTermsIndex + termPrivacyAndTerms.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        termsAndPrivacy.setText(spanContent);
        termsAndPrivacy.setMovementMethod(LinkMovementMethod.getInstance());

        Intent redirectToTermsIntent = redirectToTerms(this);
        if (redirectToTermsIntent != null) {
            termsAndPrivacy.setOnClickListener(v -> {
                startActivity(redirectToTermsIntent);
                analyticsManager.trackUiClick(AnalyticsObject.PRIVACY_POLICY, AnalyticsLocation.fromConnectButtonWithId(connection.id));
            });
        }

        View manageConnectionView = findViewById(R.id.ifttt_manage_connection);
        View googlePlayView = findViewById(R.id.google_play_link);

        PackageManager packageManager = getPackageManager();
        boolean hasConnected =
                connection.status == Connection.Status.enabled || connection.status == Connection.Status.disabled;
        if (hasConnected || ButtonUiHelper.isIftttInstalled(packageManager)) {
            googlePlayView.setVisibility(View.GONE);

            Intent manageIntent = ButtonApiHelper.redirectToManage(this, connection.id);
            if (manageIntent != null && hasConnected) {
                manageConnectionView.setVisibility(View.VISIBLE);
                manageConnectionView.setOnClickListener(v -> {
                    startActivity(manageIntent);
                    analyticsManager.trackUiClick(AnalyticsObject.MANAGE_CONNECTION, AnalyticsLocation.fromConnectButtonWithId(connection.id));
                });
            } else {
                manageConnectionView.setVisibility(View.GONE);
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

        analyticsManager.trackUiImpression(AnalyticsObject.CONNECT_INFORMATION_MODAL, AnalyticsLocation.fromConnectButtonWithId(connection.id));
    }

    private static final String EXTRA_CONNECTION = "extra_connection";

    public static Intent intent(Context context, Connection connection) {
        return new Intent(context, AboutIftttActivity.class).putExtra(EXTRA_CONNECTION, connection);
    }
}
