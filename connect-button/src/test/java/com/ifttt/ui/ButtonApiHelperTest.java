package com.ifttt.ui;

import android.net.Uri;
import com.ifttt.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;
import static com.ifttt.ui.IftttConnectButton.ButtonState.CreateAccount;
import static com.ifttt.ui.IftttConnectButton.ButtonState.Initial;
import static com.ifttt.ui.IftttConnectButton.ButtonState.Login;
import static com.ifttt.ui.IftttConnectButton.ButtonState.ServiceAuthentication;
import static java.util.Collections.emptyList;

@RunWith(RobolectricTestRunner.class)
public final class ButtonApiHelperTest {

    private final Connection connection = new Connection("", "", "", Connection.Status.never_enabled, "", emptyList());

    @Test
    public void testRequiredFields() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, Initial, "http://redirect", Collections.emptyList(),
                "abc@efg.com", "", "auth_code", null);

        assertThat(uri.getQueryParameter("sdk_return_to")).isEqualTo("http://redirect");
        assertThat(uri.getQueryParameter("email")).isEqualTo("abc@efg.com");
        assertThat(uri.getQueryParameter("invite_code")).isNull();
    }

    @Test
    public void testInviteCode() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, Initial, "http://redirect", Collections.emptyList(),
                "abc@efg.com", "", "auth_code", "abcd");
        assertThat(uri.getQueryParameter("invite_code")).isEqualTo("abcd");
    }

    @Test
    public void testOAuthCode() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, Initial, "http://redirect", Collections.emptyList(),
                "abc@efg.com", "", "auth_code", "abcd");
        assertThat(uri.getQueryParameter("code")).isNull();

        Uri uri1 = ButtonApiHelper.getEmbedUri(connection, CreateAccount, "http://redirect", Collections.emptyList(),
                "abc@efg.com", "", "auth_code", "abcd");
        assertThat(uri1.getQueryParameter("code")).isEqualTo("auth_code");

        Uri uri2 = ButtonApiHelper.getEmbedUri(connection, Login, "http://redirect", Collections.emptyList(),
                "abc@efg.com", "", "auth_code", "abcd");
        assertThat(uri2.getQueryParameter("code")).isEqualTo("auth_code");
    }

    @Test
    public void testServiceAuthentication() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, Initial, "http://redirect", Collections.emptyList(),
                "abc@efg.com", "", "auth_code", "abcd");
        assertThat(uri.getQueryParameter("skip_sdk_redirect")).isNull();

        Uri uri1 = ButtonApiHelper.getEmbedUri(connection, ServiceAuthentication, "http://redirect",
                Collections.emptyList(), "abc@efg.com", "", "auth_code", "abcd");
        assertThat(uri1.getQueryParameter("skip_sdk_redirect")).isEqualTo("true");
    }

    @Test
    public void testSdkCreateAccount() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, Initial, "http://redirect", Collections.emptyList(),
                "abc@efg.com", "", "auth_code", "abcd");
        assertThat(uri.getQueryParameter("sdk_create_account")).isNull();

        Uri uri1 = ButtonApiHelper.getEmbedUri(connection, CreateAccount, "http://redirect", Collections.emptyList(),
                "abc@efg.com", "", "auth_code", "abcd");
        assertThat(uri1.getQueryParameter("sdk_create_account")).isEqualTo("true");
    }

    @Test
    public void testEmailAppsDetectorWhenLogin() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, Login, "http://redirect", Arrays.asList("a", "b"),
                "abc@efg.com", "", "auth_code", "abcd");
        List<String> params = uri.getQueryParameters("available_email_app_schemes[]");
        assertThat(params).hasSize(2);
        assertThat(params.get(0)).isEqualTo("a");
        assertThat(params.get(1)).isEqualTo("b");
    }

    @Test
    public void testEmailAppsDetectorWhenCreateAccount() {
        Uri uri = ButtonApiHelper.getEmbedUri(connection, CreateAccount, "http://redirect", Arrays.asList("a", "b"),
                "abc@efg.com", "", "auth_code", "abcd");
        List<String> params = uri.getQueryParameters("available_email_app_schemes[]");
        assertThat(params).hasSize(0);
    }
}
