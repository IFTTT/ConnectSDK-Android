package com.ifttt.location;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.ConnectionApiClient;
import com.ifttt.connect.api.Feature;
import com.ifttt.connect.api.FeatureStep;
import com.ifttt.connect.api.LocationFieldValue;
import com.ifttt.connect.api.Service;
import com.ifttt.connect.api.StringFieldValue;
import com.ifttt.connect.api.UserFeature;
import com.ifttt.connect.api.UserFeatureField;
import com.ifttt.connect.api.UserFeatureStep;
import com.ifttt.connect.ui.ConnectButton;
import com.ifttt.connect.ui.CredentialsProvider;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

@RunWith(AndroidJUnit4.class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = 28)
public class ConnectLocationTest {

    private ConnectButton button;
    private ConnectionApiClient apiClient;

    private static final Connection NON_LOCATION_CONNECTION = new Connection("id",
        "name",
        "description",
        Connection.Status.enabled,
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
                    Collections.singletonList(new UserFeatureField<>(new StringFieldValue(""), "TEXT_FIELD", "fieldId"))
                ))
            ))
        ))
    );

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
        ConnectLocation.getInstance().setUpWithConnectButton(button, new LocationStatusCallbackAdapter() {
        });
        fail();
    }

    @Test
    public void shouldPromptPermission() {
        Connection connection = connection(Connection.Status.enabled);

        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation(new GeofenceProviderAdapter() {
            @Override
            public void updateGeofences(
                Connection connection, @Nullable ConnectLocation.LocationStatusCallback locationStatusCallback
            ) {
                fail();
            }
        }, apiClient);
        location.setUpWithConnectButton(button, new LocationStatusCallbackAdapter() {
            @Override
            public void onRequestLocationPermission() {
                ref.set(true);
            }
        });
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
        ShadowApplication application = shadowOf((Application) ApplicationProvider.getApplicationContext());
        application.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        Connection connection = connection(Connection.Status.enabled);

        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation(new GeofenceProviderAdapter() {
            @Override
            public void updateGeofences(
                Connection connection, @Nullable ConnectLocation.LocationStatusCallback locationStatusCallback
            ) {
                ref.set(true);
            }
        }, apiClient);
        location.setUpWithConnectButton(button, new LocationStatusCallbackAdapter() {
        });
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
        ShadowApplication application = shadowOf((Application) ApplicationProvider.getApplicationContext());
        application.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        Connection connection = connection(Connection.Status.disabled);

        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation(new GeofenceProviderAdapter() {
            @Override
            public void removeGeofences(@Nullable ConnectLocation.LocationStatusCallback locationStatusCallback) {
                ref.set(true);
            }
        }, apiClient);
        location.setUpWithConnectButton(button, new LocationStatusCallbackAdapter() {
        });
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
        ShadowApplication application = shadowOf((Application) ApplicationProvider.getApplicationContext());
        application.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        Connection connection = connection(Connection.Status.never_enabled);

        AtomicBoolean ref = new AtomicBoolean();
        ConnectLocation location = new ConnectLocation(new GeofenceProviderAdapter() {
            @Override
            public void removeGeofences(@Nullable ConnectLocation.LocationStatusCallback locationStatusCallback) {
                ref.set(true);
            }
        }, apiClient);
        location.setUpWithConnectButton(button, new LocationStatusCallbackAdapter() {
        });
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
    public void shouldNotInvokeCallbackForNonLocationConnection() {
        ConnectLocation location = new ConnectLocation(new GeofenceProviderAdapter() {
        }, apiClient);
        location.setUpWithConnectButton(button, new LocationStatusCallbackAdapter() {
            @Override
            public void onRequestLocationPermission() {
                fail();
            }

            @Override
            public void onLocationStatusUpdated(boolean activated) {
                fail();
            }
        });
        button.setup(ConnectButton.Configuration.newBuilder("email@ifttt.com", Uri.EMPTY).withConnection(
            NON_LOCATION_CONNECTION).withCredentialProvider(new CredentialsProvider() {
            @Override
            public String getOAuthCode() {
                return "";
            }

            @Override
            public String getUserToken() {
                return "";
            }
        }).build());
    }

    @Test
    public void shouldKeepLocationEventListener() {
        CredentialsProvider provider = new CredentialsProvider() {
            @Override
            public String getOAuthCode() {
                return "";
            }

            @Override
            public String getUserToken() {
                return "";
            }
        };
        Context context = button.getContext();
        ConnectLocation location = ConnectLocation.init(
            context,
            new ConnectionApiClient.Builder(context, provider).build()
        );

        AtomicBoolean ref = new AtomicBoolean();
        location.setLocationEventListener((type, data) -> {
            ref.set(true);
        });

        location = ConnectLocation.init(context);
        assertThat(location.locationEventListener).isNotNull();
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
        public void updateGeofences(
            Connection connection, @Nullable ConnectLocation.LocationStatusCallback locationStatusCallback
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeGeofences(@Nullable ConnectLocation.LocationStatusCallback locationStatusCallback) {
            throw new UnsupportedOperationException();
        }
    }

    private abstract static class LocationStatusCallbackAdapter implements ConnectLocation.LocationStatusCallback {
        @Override
        public void onRequestLocationPermission() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onLocationStatusUpdated(boolean activated) {
            throw new UnsupportedOperationException();
        }
    }
}
