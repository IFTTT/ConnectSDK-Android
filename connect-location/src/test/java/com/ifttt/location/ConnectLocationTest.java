package com.ifttt.location;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.WorkManager;
import com.google.common.truth.Truth;
import com.ifttt.connect.Connection;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.ui.CredentialsProvider;
import com.ifttt.connect.ui.ConnectButton;
import com.ifttt.connect.ui.ConnectButtonState;
import java.util.Collections;
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
    private CredentialsProvider credentialsProvider;
    private WorkManager workManager;

    @Before
    public void setUp() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);
        scenario.onActivity(activity -> {
            button = new ConnectButton(activity);
            workManager = WorkManager.getInstance(activity);
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

        credentialsProvider = new CredentialsProvider() {
            @Override
            public String getOAuthCode() {
                return null;
            }

            @Override
            public String getUserToken() {
                return null;
            }
        };
        apiClient = new ConnectionApiClient.Builder(button.getContext()).setCredentialsProvider(credentialsProvider)
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void setUpWithoutInit() {
        ConnectLocation.getInstance().setUpWithConnectButton(button);
        fail();
    }

    @Test
    public void shouldUpdateGeofencesWhenEnabled() {
        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation("id", connection -> ref.set(true), apiClient, workManager);

        location.onStateChanged(ConnectButtonState.Enabled, ConnectButtonState.Initial, connection);
        Truth.assertThat(ref.get()).isTrue();
    }

    @Test
    public void shouldUpdateGeofencesWhenDisabled() {
        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation("id", connection -> ref.set(true), apiClient, workManager);

        location.onStateChanged(ConnectButtonState.Disabled, ConnectButtonState.Enabled, connection);
        Truth.assertThat(ref.get()).isTrue();
    }

    @Test
    public void shouldUpdateGeofencesWhenInitial() {
        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation("id", connection -> ref.set(true), apiClient, workManager);

        location.onStateChanged(ConnectButtonState.Initial, ConnectButtonState.Enabled, connection);
        Truth.assertThat(ref.get()).isTrue();
    }

    @Test
    public void shouldNotUpdateGeofencesWhenCreateAccountOrLogin() {
        ConnectLocation location = new ConnectLocation("id", connection -> fail(), apiClient, workManager);

        location.onStateChanged(ConnectButtonState.CreateAccount, ConnectButtonState.Initial, connection);
        location.onStateChanged(ConnectButtonState.Login, ConnectButtonState.Initial, connection);
    }
}
