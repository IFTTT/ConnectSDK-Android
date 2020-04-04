package com.ifttt.location;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.common.truth.Truth;
import com.ifttt.connect.Connection;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.Feature;
import com.ifttt.connect.ui.ConnectButton;
import com.ifttt.connect.ui.ConnectButtonState;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = 28)
public class ConnectLocationTest {

    private ConnectButton button;
    private Connection connection;
    private ConnectionApiClient apiClient;

    @Before
    public void setUp() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);
        scenario.onActivity(activity -> {
            button = new ConnectButton(activity);
        });

        connection = new Connection("id",
            "name",
            "description",
            Connection.Status.enabled,
            "url",
            Collections.emptyList(),
            null,
            Collections.emptyList()
        );

        apiClient = new ConnectionApiClient.Builder(button.getContext()).build();
    }

    @Test(expected = IllegalStateException.class)
    public void setUpWithoutInit() {
        ConnectLocation.getInstance().setUpWithConnectButton(button);
        fail();
    }

    @Test
    public void shouldUpdateGeofencesWhenEnabled() {
        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation(features -> ref.set(true),
            new ConnectionApiClient.Builder(button.getContext()).build()
        );

        location.onStateChanged(ConnectButtonState.Enabled, ConnectButtonState.Initial, connection);
        Truth.assertThat(ref.get()).isTrue();
    }

    @Test
    public void shouldUpdateGeofencesWhenDisabled() {
        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation(features -> ref.set(true), apiClient);

        location.onStateChanged(ConnectButtonState.Disabled, ConnectButtonState.Enabled, connection);
        Truth.assertThat(ref.get()).isTrue();
    }

    @Test
    public void shouldUpdateGeofencesWhenInitial() {
        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation(features -> ref.set(true),
            new ConnectionApiClient.Builder(button.getContext()).build()
        );

        location.onStateChanged(ConnectButtonState.Initial, ConnectButtonState.Enabled, connection);
        Truth.assertThat(ref.get()).isTrue();
    }

    @Test
    public void shouldNotUpdateGeofencesWhenCreateAccountOrLogin() {
        ConnectLocation location = new ConnectLocation(new GeofenceProvider() {
            @Override
            public void updateGeofences(List<Feature> features) {
                fail();
            }
        }, new ConnectionApiClient.Builder(button.getContext()).build());

        location.onStateChanged(ConnectButtonState.CreateAccount, ConnectButtonState.Initial, connection);
        location.onStateChanged(ConnectButtonState.Login, ConnectButtonState.Initial, connection);
    }
}
