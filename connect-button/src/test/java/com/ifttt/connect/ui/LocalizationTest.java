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

    @Test
    public void testCzechLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("cs"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testDanishLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("da"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testGermanLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("de"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testSpanishLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("es"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testSpanishEnglishLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("es-419"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testFinnishLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("fi"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testFrenchLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("fr"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testFrenchCanadaLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("fr-CA"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testItalianLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("it"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testJapaneseLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("ja"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testKoreanLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("ko"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testNorwegianLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("nb"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testDutchLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("nl"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testPolishLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("pl"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testPortugueseBrazilLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("pt-BR"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testPortuguesePortugalLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("pt-PT"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testRussianLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("ru"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testSwedishLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("sv"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testSimplifiedChineseLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("zh-CN"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }

    @Test
    public void testTraditionalChineseLocale() {
        Configuration configuration = new Configuration(activity.getBaseContext().getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag("zh-TW"));
        Context context = activity.getBaseContext().createConfigurationContext(configuration);

        String termsAndPrivacyTerm = context.getString(R.string.term_privacy_and_terms);
        String aboutIftttString = context.getString(R.string.about_ifttt_privacy_and_terms);

        String learnMoreTerm = context.getString(R.string.term_learn_more);
        String securedWithIftttString = context.getString(R.string.secured_with_ifttt);

        assertThat(aboutIftttString.contains(termsAndPrivacyTerm)).isTrue();
        assertThat(securedWithIftttString.contains(learnMoreTerm)).isTrue();
    }
}
