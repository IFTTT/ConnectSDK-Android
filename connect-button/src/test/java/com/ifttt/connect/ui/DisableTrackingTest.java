package com.ifttt.connect.ui;

import android.app.Activity;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ifttt.connect.Connection;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.CoverImage;
import com.ifttt.connect.Service;
import com.ifttt.connect.ShadowAnimatorSet;
import com.ifttt.connect.ValueProposition;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
@Config(shadows = { ShadowAnimatorSet.class })
public class DisableTrackingTest {

    private BaseConnectButton baseConnectButton ;
    private Connection connection;

    private final Activity activity = Robolectric.buildActivity(TestActivity.class).create().get();

    @Before
    public void setUp() {
        ConnectionApiClient client = new ConnectionApiClient.Builder(activity).build();
        CredentialsProvider provider = new CredentialsProvider() {
            @Override
            public String getOAuthCode() {
                return null;
            }

            @Override
            public String getUserToken() {
                return null;
            }
        };

        AnalyticsManager.destroy();
        baseConnectButton = new BaseConnectButton(activity);

        List<Service> serviceList = new ArrayList<Service>();
        serviceList.add(new Service("id1", "name1", "sname1", true, "https://google.com", 0, ""));
        serviceList.add(new Service("id2", "name2", "sname2", false, "https://google.com", 0, ""));

        connection = new Connection("id", "name", "description", Connection.Status.enabled, "url",
                serviceList, new CoverImage("", "", "", "", "", ""),
                new ArrayList<ValueProposition>());

        ConnectButton.Configuration configuration =
                ConnectButton.Configuration.Builder.withConnectionId("id", "email", provider,
                        Uri.EMPTY)
                        .disableAnalyticsTracking()
                        .build();


        new ConnectButton(activity).setup(configuration);

        baseConnectButton.setup("email", client, Uri.EMPTY, provider, "");
    }

    @Test
    public void testTrackingDisable() {
        baseConnectButton.setConnection(connection);

        List<AnalyticsEventPayload> payloads = AnalyticsManager.getInstance(activity).performRead();
        assertThat(payloads.size()).isEqualTo(0);
    }
}
