package com.ifttt.location;

import android.Manifest;
import android.app.Application;
import android.graphics.Color;
import android.net.Uri;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.ConnectionApiClient;
import com.ifttt.connect.api.Feature;
import com.ifttt.connect.api.FeatureStep;
import com.ifttt.connect.api.LocationFieldValue;
import com.ifttt.connect.api.Service;
import com.ifttt.connect.api.UserFeature;
import com.ifttt.connect.api.UserFeatureField;
import com.ifttt.connect.api.UserFeatureStep;
import com.ifttt.connect.ui.ConnectButton;
import com.ifttt.connect.ui.CredentialsProvider;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = 28)
public class ConnectLocationTest {

    private ConnectButton button;
    private ConnectionApiClient apiClient;

    @Before
    public void setUp() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);
        scenario.onActivity(activity -> {
            button = new ConnectButton(activity);
        });

        CredentialsProvider credentialsProvider = new CredentialsProvider() {
            @Override
            public String getOAuthCode() {
                return null;
            }

            @Override
            public String getUserToken() {
                return null;
            }
        };
        apiClient = new ConnectionApiClient.Builder(button.getContext(), credentialsProvider).build();
    }

    @Test(expected = IllegalStateException.class)
    public void setUpWithoutInit() {
        ConnectLocation.getInstance().setUpWithConnectButton(button, () -> {
        });
        fail();
    }

    @Test
    public void shouldPromptPermission() {
        Connection connection = connection(Connection.Status.enabled);

        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation(new GeofenceProviderAdapter() {
            @Override
            public void updateGeofences(Connection connection) {
                fail();
            }
        }, apiClient);
        location.setUpWithConnectButton(button, () -> ref.set(true));
        button.setup(ConnectButton.Configuration.newBuilder("email@ifttt.com", Uri.EMPTY)
            .withConnection(connection)
            .withCredentialProvider(new CredentialsProvider() {
                @Override
                public String getOAuthCode() {
                    return "";
                }

                @Override
                public String getUserToken() {
                    return "";
                }
            })
            .build());

        assertThat(ref.get()).isTrue();
    }

    @Test
    public void shouldUpdateGeofenceAfterPermissionGranted() {
        ShadowApplication application = Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext());
        application.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        Connection connection = connection(Connection.Status.enabled);

        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation(new GeofenceProviderAdapter() {
            @Override
            public void updateGeofences(Connection connection) {
                ref.set(true);
            }
        }, apiClient);
        location.setUpWithConnectButton(button, Assert::fail);
        button.setup(ConnectButton.Configuration.newBuilder("email@ifttt.com", Uri.EMPTY)
            .withConnection(connection)
            .withCredentialProvider(new CredentialsProvider() {
                @Override
                public String getOAuthCode() {
                    return "";
                }

                @Override
                public String getUserToken() {
                    return "";
                }
            })
            .build());

        assertThat(ref.get()).isTrue();
    }

    @Test
    public void shouldUpdateGeofencesWhenDisabled() {
        ShadowApplication application = Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext());
        application.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        Connection connection = connection(Connection.Status.disabled);

        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation(new GeofenceProviderAdapter() {
            @Override
            public void removeGeofences() {
                ref.set(true);
            }
        }, apiClient);
        location.setUpWithConnectButton(button, Assert::fail);
        button.setup(ConnectButton.Configuration.newBuilder("email@ifttt.com", Uri.EMPTY)
            .withConnection(connection)
            .withCredentialProvider(new CredentialsProvider() {
                @Override
                public String getOAuthCode() {
                    return "";
                }

                @Override
                public String getUserToken() {
                    return "";
                }
            })
            .build());

        assertThat(ref.get()).isTrue();
    }

    @Test
    public void shouldUpdateGeofencesWhenInitial() {
        ShadowApplication application = Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext());
        application.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        Connection connection = connection(Connection.Status.never_enabled);

        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation(new GeofenceProviderAdapter() {
            @Override
            public void removeGeofences() {
                ref.set(true);
            }
        }, apiClient);
        location.setUpWithConnectButton(button, Assert::fail);
        button.setup(ConnectButton.Configuration.newBuilder("email@ifttt.com", Uri.EMPTY)
            .withConnection(connection)
            .withCredentialProvider(new CredentialsProvider() {
                @Override
                public String getOAuthCode() {
                    return "";
                }

                @Override
                public String getUserToken() {
                    return "";
                }
            })
            .build());

        assertThat(ref.get()).isTrue();
    }

    private Connection connection(Connection.Status status) {
        return new Connection("id",
            "name",
            "description",
            status,
            "url",
            Collections.singletonList(new Service("id",
                "name",
                "shortName",
                true,
                "http://image.com/image",
                Color.BLACK,
                ""
            )),
            null,
            Collections.singletonList(new Feature("id",
                "title",
                "description",
                "icon",
                Collections.singletonList(new UserFeature("id",
                    "featureId",
                    true,
                    Collections.singletonList(new UserFeatureStep(FeatureStep.StepType.Trigger,
                        "id",
                        "stepId",
                        Collections.singletonList(new UserFeatureField<>(new LocationFieldValue(0.0D, 0.0D, 100D, ""),
                            "LOCATION_ENTER",
                            "fieldId"
                        ))
                    ))
                ))
            ))
        );
    }

    private abstract static class GeofenceProviderAdapter implements GeofenceProvider {
        @Override
        public void updateGeofences(Connection connection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeGeofences() {
            throw new UnsupportedOperationException();
        }
    }
}
