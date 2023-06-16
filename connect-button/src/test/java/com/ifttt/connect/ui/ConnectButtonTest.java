package com.ifttt.connect.ui;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.net.Uri;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ifttt.connect.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28)
public final class ConnectButtonTest {

    private Activity activity;

    @Before
    public void setUp() {
        ActivityController<TestActivity> controller = Robolectric.buildActivity(TestActivity.class);
        controller.get().setTheme(R.style.Base_Theme_AppCompat);
        controller.create().start();

        activity = controller.get();
    }

    @Test
    public void shouldHideUiWithInvalidEmail() {
        ConnectButton connectButton = new ConnectButton(activity);
        connectButton.setup(ConnectButton.Configuration.newBuilder("Not a valid email", Uri.EMPTY).withConnectionId(
            "123").withCredentialProvider(new CredentialsProvider() {
            @Override
            public String getOAuthCode() {
                return null;
            }

            @Override
            public String getUserToken() {
                return null;
            }
        }).build());

        for (int i = 0; i < connectButton.getChildCount(); i++) {
            View child = connectButton.getChildAt(i);
            assertThat(child.getVisibility()).isEqualTo(View.GONE);
        }
    }

    @Test
    public void shouldShowUiWithValidEmail() {
        ConnectButton connectButton = new ConnectButton(activity);
        connectButton.setup(ConnectButton.Configuration.newBuilder("email@ifttt.com", Uri.EMPTY)
            .withConnectionId("123")
            .withCredentialProvider(new CredentialsProvider() {
                @Override
                public String getOAuthCode() {
                    return null;
                }

                @Override
                public String getUserToken() {
                    return null;
                }
            })
            .build());

        for (int i = 0; i < connectButton.getChildCount(); i++) {
            View child = connectButton.getChildAt(i);
            assertThat(child.getVisibility()).isEqualTo(View.VISIBLE);
        }
    }
}
