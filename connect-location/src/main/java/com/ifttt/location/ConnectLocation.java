package com.ifttt.location;

import android.content.Context;
import androidx.annotation.Nullable;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.CredentialsProvider;
import com.ifttt.connect.Feature;
import com.ifttt.connect.LocationFieldValue;
import com.ifttt.connect.UserFeatureField;
import com.ifttt.connect.ui.ConnectButton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ConnectLocation implements ConnectButton.onConnectionStateChangeListener {

    private GeofenceProvider geofenceProvider;
    private ConnectionApiClient connectionApiClient;

    final static String FIELD_TYPE_LOCATION_ENTER = "location/triggers.enter_region_location";
    final static String FIELD_TYPE_LOCATION_EXIT = "location/triggers.exit_region_location";
    final static String FIELD_TYPE_LOCATION_ENTER_EXIT =
            "location/triggers.enter_or_exit_region_location";

    final static List<String> locationFieldTypesList = Arrays.asList(FIELD_TYPE_LOCATION_ENTER, FIELD_TYPE_LOCATION_EXIT, FIELD_TYPE_LOCATION_ENTER_EXIT);

    public ConnectLocation(Context context, Configuration configuration) {
        geofenceProvider = new GeofenceProvider(context);
        if (configuration.connectionApiClient == null) {
            ConnectionApiClient.Builder clientBuilder = new ConnectionApiClient.Builder(context);
            connectionApiClient = clientBuilder.build();
        } else {
            connectionApiClient = configuration.connectionApiClient;
        }
        // Set up work manager to poll
    }

    @Override
    public void onConnectionEnabled(List<Feature> connectionFeatures) {
        // Filter the location based enabled user feature fields.
        // Using hard-coded data till API is ready

        List<UserFeatureField> userFeatureFields = new ArrayList<>();
        userFeatureFields.add(new UserFeatureField(
                new LocationFieldValue(0, 0, 0.0, "address"), "id")
        );

        geofenceProvider.updateGeofences(userFeatureFields);
    }

    @Override
    public void onConnectionDisabled() {
        // Remove all previously registered geofences.
        geofenceProvider.updateGeofences(Collections.emptyList());
    }

    public static final class Configuration {

        @Nullable private final ConnectionApiClient connectionApiClient;
        private String connectionId;
        private final CredentialsProvider credentialsProvider;

        /**
         * Builder class for constructing a Configuration object.
         */
        public static final class Builder {
            private final CredentialsProvider credentialsProvider;
            @Nullable private ConnectionApiClient connectionApiClient;

            private String connectionId;

            /**
             * Factory method for creating a new Configuration builder.
             *
             * @param connectionId A Connection id that the {@link ConnectionApiClient} can use to fetch the
             * associated Connection object.
             * @param credentialsProvider {@link CredentialsProvider} object that helps get the user token
             */
            public static Builder withConnectionId(String connectionId, CredentialsProvider credentialsProvider) {
                Builder builder = new Builder(credentialsProvider);
                builder.connectionId = connectionId;
                return builder;
            }

            private Builder(CredentialsProvider credentialsProvider) {
                this.credentialsProvider = credentialsProvider;
            }

            /**
             * @param connectionApiClient an optional {@link ConnectionApiClient} that will be used for the ConnectLocation
             * instead of the default one.
             * @return The Builder object itself for chaining.
             */
            public Builder setConnectionApiClient(ConnectionApiClient connectionApiClient) {
                this.connectionApiClient = connectionApiClient;
                return this;
            }

            public Configuration build() {
                Configuration configuration =
                        new Configuration(credentialsProvider, connectionApiClient);
                configuration.connectionId = connectionId;
                return configuration;
            }
        }

        private Configuration(CredentialsProvider credentialsProvider, @Nullable ConnectionApiClient connectionApiClient) {
            this.credentialsProvider = credentialsProvider;
            this.connectionApiClient = connectionApiClient;
        }
    }
}
