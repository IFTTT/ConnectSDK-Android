package com.ifttt.connect.api;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class ConnectionApiClientTest {

    @Test
    public void newBuilderShouldOverrideUserTokenProvider() {
        UserTokenProvider oldProvider = () -> null;
        Context context = ApplicationProvider.getApplicationContext();
        ConnectionApiClient client = new ConnectionApiClient.Builder(context, oldProvider).build();

        UserTokenProvider newProvider = () -> "token";
        ConnectionApiClient newClient = client.newBuilder(newProvider).build();

        assertThat(client).isNotSameInstanceAs(newClient);
        assertThat(client.userTokenProvider).isNotSameInstanceAs(newClient.userTokenProvider);
    }
}
