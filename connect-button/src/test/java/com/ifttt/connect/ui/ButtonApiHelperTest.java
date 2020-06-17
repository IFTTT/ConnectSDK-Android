package com.ifttt.connect.ui;

import android.net.Uri;
import com.ifttt.connect.api.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;
import static com.ifttt.connect.ui.ConnectButtonState.CreateAccount;
import static com.ifttt.connect.ui.ConnectButtonState.Initial;
import static com.ifttt.connect.ui.ConnectButtonState.Login;
import static java.util.Collections.emptyList;

@RunWith(RobolectricTestRunner.class)
public final class ButtonApiHelperTest {

    private final Connection connection =
            new Connection("", "", "", Connection.Status.never_enabled, "", emptyList(), null, emptyList());
    private final Uri redirectUri = Uri.parse("http://redirect");

    @Test
    public void testRequiredFields() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, Initial, redirectUri, Collections.emptyList(), "abc@efg.com",
                null, "", "auth_code", null, false
        );

        assertThat(uri.getQueryParameter("sdk_return_to")).isEqualTo("http://redirect");
        assertThat(uri.getQueryParameter("email")).isEqualTo("abc@efg.com");
        assertThat(uri.getQueryParameter("invite_code")).isNull();
    }

    @Test
    public void testInviteCode() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, Initial, redirectUri, Collections.emptyList(), "abc@efg.com",
                null, "", "auth_code", "abcd", false
        );
        assertThat(uri.getQueryParameter("invite_code")).isEqualTo("abcd");
    }

    @Test
    public void testOAuthCode() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, Initial, redirectUri, Collections.emptyList(), "abc@efg.com",
                null, "", "auth_code", "abcd", false
        );
        assertThat(uri.getQueryParameter("code")).isNull();

        Uri uri1 = ButtonApiHelper.getEmbedUri(connection, CreateAccount, redirectUri, Collections.emptyList(),
                "abc@efg.com", null, "", "auth_code", "abcd", false
        );
        assertThat(uri1.getQueryParameter("code")).isEqualTo("auth_code");

        Uri uri2 = ButtonApiHelper.getEmbedUri(connection, Login, redirectUri, Collections.emptyList(), "abc@efg.com",
                null, "", "auth_code", "abcd", false
        );
        assertThat(uri2.getQueryParameter("code")).isEqualTo("auth_code");
    }

    @Test
    public void testSdkCreateAccount() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, Initial, redirectUri, Collections.emptyList(), "abc@efg.com",
                null, "", "auth_code", "abcd", false
        );
        assertThat(uri.getQueryParameter("sdk_create_account")).isNull();

        Uri uri1 = ButtonApiHelper.getEmbedUri(connection, CreateAccount, redirectUri, Collections.emptyList(),
                "abc@efg.com", null, "", "auth_code", "abcd", false
        );
        assertThat(uri1.getQueryParameter("sdk_create_account")).isEqualTo("true");
    }

    @Test
    public void testUsername() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, Initial, redirectUri, Collections.emptyList(), "abc@efg.com",
                "user_name", "", "auth_code", "abcd", false
        );
        assertThat(uri.getQueryParameter("username")).isEqualTo("user_name");
        assertThat(uri.getQueryParameter("email")).isNull();
    }

    @Test
    public void testEmailAppsDetectorWhenLogin() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, Login, redirectUri, Arrays.asList("a", "b"), "abc@efg.com",
                null, "", "auth_code", "abcd", false
        );
        List<String> params = uri.getQueryParameters("available_email_app_schemes[]");
        assertThat(params).hasSize(2);
        assertThat(params.get(0)).isEqualTo("a");
        assertThat(params.get(1)).isEqualTo("b");
    }

    @Test
    public void testEmailAppsDetectorWhenCreateAccount() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, CreateAccount, redirectUri, Arrays.asList("a", "b"),
                "abc@efg.com", null, "", "auth_code", "abcd", false
        );
        List<String> params = uri.getQueryParameters("available_email_app_schemes[]");
        assertThat(params).hasSize(0);
    }

    @Test
    public void testSkipConfigurationFlag() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, CreateAccount, redirectUri, Arrays.asList("a", "b"),
            "abc@efg.com", null, "", "auth_code", "abcd", true
        );

        assertThat(uri.getQueryParameter("skip_config")).isEqualTo("true");
    }
}
