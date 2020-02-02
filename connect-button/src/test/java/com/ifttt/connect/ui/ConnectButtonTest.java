package com.ifttt.connect.ui;

import android.app.Activity;
import android.net.Uri;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public final class ConnectButtonTest {

    private final Activity activity = Robolectric.buildActivity(TestActivity.class).get();

    @Before
    public void setUp() {
        ActivityScenario.launch(TestActivity.class);
    }

    @Test
    public void shouldHideUiWithInvalidEmail() {
        ConnectButton connectButton = new ConnectButton(activity);
        connectButton.setup(ConnectButton.Configuration.Builder.withConnectionId("123", "Not a valid email",
                new CredentialsProvider() {
                    @Override
                    public String getOAuthCode() {
                        return null;
                    }

                    @Override
                    public String getUserToken() {
                        return null;
                    }
                }, Uri.EMPTY).build());

        for (int i = 0; i < connectButton.getChildCount(); i++) {
            View child = connectButton.getChildAt(i);
            assertThat(child.getVisibility()).isEqualTo(View.GONE);
        }
    }

    @Test
    public void shouldShowUiWithValidEmail() {
        ConnectButton connectButton = new ConnectButton(activity);
        connectButton.setup(ConnectButton.Configuration.Builder.withConnectionId("123", "email@ifttt.com",
                new CredentialsProvider() {
                    @Override
                    public String getOAuthCode() {
                        return null;
                    }

                    @Override
                    public String getUserToken() {
                        return null;
                    }
                }, Uri.EMPTY).build());

        for (int i = 0; i < connectButton.getChildCount(); i++) {
            View child = connectButton.getChildAt(i);
            assertThat(child.getVisibility()).isEqualTo(View.VISIBLE);
        }
    }
}
