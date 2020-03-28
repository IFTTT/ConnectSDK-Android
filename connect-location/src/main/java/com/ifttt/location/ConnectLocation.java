package com.ifttt.location;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import com.ifttt.connect.Connection;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.ErrorResponse;
import com.ifttt.connect.LocationFieldValue;
import com.ifttt.connect.api.PendingResult;
import java.util.ArrayList;
import java.util.List;

public final class ConnectLocation {

    private Context context;
    private static ConnectionApiClient API_CLIENT;

    final static String TRIGGER_ID_LOCATION_ENTER = "location/triggers.enter_region_location";
    final static String TRIGGER_ID_LOCATION_EXIT = "location/triggers.exit_region_location";
    final static String TRIGGER_ID_LOCATION_ENTER_EXIT = "location/triggers.enter_or_exit_region_location";

    public ConnectLocation(Context context) {
        this.context = context;
    }

    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    public void setUp(LocationConfiguration configuration) {
        if (configuration.connectionApiClient == null) {
            if (API_CLIENT == null) {
                ConnectionApiClient.Builder clientBuilder = new ConnectionApiClient.Builder(context);
                if (configuration.inviteCode != null) {
                    clientBuilder.setInviteCode(configuration.inviteCode);
                }
                API_CLIENT = clientBuilder.build();
            }
        } else {
            API_CLIENT = configuration.connectionApiClient;
        }

        UserTokenAsyncTask task = new UserTokenAsyncTask(configuration.credentialsProvider, () -> {
            if (configuration.connection != null) {
                setUpGeofenceForLocationFields(configuration.connection);
                return;
            }

            if (configuration.connectionId == null) {
                throw new IllegalStateException("Connection id cannot be null.");
            }

            PendingResult<Connection> pendingResult = API_CLIENT.api().showConnection(configuration.connectionId);
            pendingResult.execute(new PendingResult.ResultCallback<Connection>() {
                @Override
                public void onSuccess(Connection result) {
                    setUpGeofenceForLocationFields(result);
                }

                @Override
                public void onFailure(ErrorResponse errorResponse) {
                    // Handle errors
                }
            });
        });
        task.execute();
    }

    private void setUpGeofenceForLocationFields(Connection connection) {
        List<LocationTriggerField> locationTriggerFields = new ArrayList<>();
        locationTriggerFields.add( new LocationTriggerField(
                "id",
                "location/triggers.enter_or_exit_region_location",
                    new LocationFieldValue(
                            0,
                            0,
                            0.0,
                            "address"
                    )
                )
        );

        // Check if the connection contains location trigger fields before setting up geofences.
        // Using hard-coded list for testing.

        GeofenceProvider geofenceProvider = new GeofenceProvider(context);
        geofenceProvider.updateGeofences(locationTriggerFields);
    }

    public static class LocationConfiguration {

        private final LocationCredentialsProvider credentialsProvider;
        @Nullable private final ConnectionApiClient connectionApiClient;
        @Nullable private String connectionId;
        @Nullable private Connection connection;
        @Nullable private String inviteCode;

        /**
         * Builder class for constructing a Configuration object.
         */
        public static final class Builder {
            private final LocationCredentialsProvider credentialsProvider;
            @Nullable private ConnectionApiClient connectionApiClient;
            @Nullable private String connectionId;
            @Nullable private Connection connection;
            @Nullable private String inviteCode;

            /**
             * Factory method for creating a new Location Configuration builder.
             *
             * @param connection {@link Connection} object.
             */
            public static Builder withConnection(Connection connection, LocationCredentialsProvider credentialsProvider) {
                Builder builder = new Builder(credentialsProvider);
                builder.connection = connection;
                return builder;
            }

            /**
             * Factory method for creating a new Location Configuration builder.
             *
             * @param connectionId A Connection id that the {@link ConnectionApiClient} can use to fetch the
             * associated Connection object.
             * @return The Builder object itself for chaining.
             */
            public static Builder withConnectionId(String connectionId, LocationCredentialsProvider credentialsProvider) {
                Builder builder = new Builder(credentialsProvider);
                builder.connectionId = connectionId;
                return builder;
            }

            private Builder(LocationCredentialsProvider credentialsProvider) {
                this.credentialsProvider = credentialsProvider;
            }

            /**
             * @param connectionApiClient an optional {@link ConnectionApiClient} that will be used for the ConnectButton
             * instead of the default one.
             * @return The Builder object itself for chaining.
             */
            public Builder setConnectionApiClient(ConnectionApiClient connectionApiClient) {
                this.connectionApiClient = connectionApiClient;
                return this;
            }

            /**
             * @param inviteCode an optional string as the invite code, this is needed if your service is not yet
             * published on IFTTT Platform.
             * @return The Builder object itself for chaining.
             * @see ConnectionApiClient.Builder#setInviteCode(String)
             */
            public Builder setInviteCode(String inviteCode) {
                this.inviteCode = inviteCode;
                return this;
            }

            public LocationConfiguration build() {
                if (connection == null && connectionId == null) {
                    throw new IllegalStateException("Either connection or connectionId must be non-null.");
                }

                LocationConfiguration configuration =
                        new LocationConfiguration(credentialsProvider, connectionApiClient);
                configuration.connection = connection;
                configuration.connectionId = connectionId;
                configuration.inviteCode = inviteCode;
                return configuration;
            }

        }

        private LocationConfiguration(LocationCredentialsProvider credentialsProvider, @Nullable ConnectionApiClient connectionApiClient) {
            this.credentialsProvider = credentialsProvider;
            this.connectionApiClient = connectionApiClient;
        }
    }

    private static final class UserTokenAsyncTask extends android.os.AsyncTask<Void, Void, String> {

        private interface UserTokenCallback {
            void onComplete();
        }

        private final LocationCredentialsProvider callback;
        private final UserTokenCallback userTokenCallback;

        private UserTokenAsyncTask(LocationCredentialsProvider callback, UserTokenCallback userTokenCallback) {
            this.callback = callback;
            this.userTokenCallback = userTokenCallback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                return callback.getUserToken();
            } catch (Exception e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }

                return null;
            }
        }

        @Override
        protected void onPostExecute(@Nullable String s) {
            if (s != null) {
                API_CLIENT.setUserToken(s);
            }

            userTokenCallback.onComplete();
        }
    }
}
