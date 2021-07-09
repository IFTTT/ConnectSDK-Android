package com.ifttt.connect.api;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public final class ConnectionApiClientTest {

    @Test
    public void newBuilderShouldOverrideUserTokenProvider() {
        UserTokenProvider oldProvider = () -> null;
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        ConnectionApiClient client = new ConnectionApiClient.Builder(context, oldProvider).build();

        UserTokenProvider newProvider = () -> "token";
        ConnectionApiClient newClient = client.newBuilder(newProvider).build();

        assertThat(client).isNotSameInstanceAs(newClient);
        assertThat(client.userTokenProvider).isNotSameInstanceAs(newClient.userTokenProvider);
    }
}
