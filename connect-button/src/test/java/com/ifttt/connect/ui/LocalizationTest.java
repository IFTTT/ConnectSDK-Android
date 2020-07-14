package com.ifttt.connect.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ifttt.connect.R;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28)
public final class LocalizationTest {

    private final Activity activity = Robolectric.buildActivity(TestActivity.class).create().get();

    @Test
    public void testNonSupportedLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("he"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        assertThat(context.getString(R.string.connect)).isEqualTo("Connect");
    }

    private Context fetchLocalizedContext(String localeTag) {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag(localeTag));
        return activity.getBaseContext().createConfigurationContext(configuration);
    }

    private boolean checkPrivacyAndTermsString(Context context) {
        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        return aboutIftttString.contains(termsAndPrivacyTerm);
    }

    private boolean checkLearnMoreString(Context context) {
        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        return securedWithIftttString.contains(learnMoreTerm);
    }

    @Test
    public void testCzechLocale() {
        Context context = fetchLocalizedContext("cs");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testDanishLocale() {
        Context context = fetchLocalizedContext("da");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testGermanLocale() {
        Context context = fetchLocalizedContext("de");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testSpanishLocale() {
        Context context = fetchLocalizedContext("es");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testSpanishEnglishLocale() {
        Context context = fetchLocalizedContext("es-419");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testFinnishLocale() {
        Context context = fetchLocalizedContext("fi");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testFrenchLocale() {
        Context context = fetchLocalizedContext("fr");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testFrenchCanadaLocale() {
        Context context = fetchLocalizedContext("fr-CA");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testItalianLocale() {
        Context context = fetchLocalizedContext("it");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testJapaneseLocale() {
        Context context = fetchLocalizedContext("ja");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testKoreanLocale() {
        Context context = fetchLocalizedContext("ko");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testNorwegianLocale() {
        Context context = fetchLocalizedContext("nb");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testDutchLocale() {
        Context context = fetchLocalizedContext("nl");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testPolishLocale() {
        Context context = fetchLocalizedContext("pl");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testPortugueseBrazilLocale() {
        Context context = fetchLocalizedContext("pt-BR");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testPortuguesePortugalLocale() {
        Context context = fetchLocalizedContext("pt-PT");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testRussianLocale() {
        Context context = fetchLocalizedContext("ru");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testSwedishLocale() {
        Context context = fetchLocalizedContext("sv");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testSimplifiedChineseLocale() {
        Context context = fetchLocalizedContext("zh-CN");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }

    @Test
    public void testTraditionalChineseLocale() {
        Context context = fetchLocalizedContext("zh-TW");

        assertThat(checkPrivacyAndTermsString(context)).isTrue();
        assertThat(checkLearnMoreString(context)).isTrue();
    }
}
