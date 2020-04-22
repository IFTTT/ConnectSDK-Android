package com.ifttt.location;

import android.content.Context;
import androidx.annotation.CheckResult;
import androidx.annotation.VisibleForTesting;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.ConnectionApiClient;
import com.ifttt.connect.api.ErrorResponse;
import com.ifttt.connect.api.Feature;
import com.ifttt.connect.api.UserFeature;
import com.ifttt.connect.api.UserFeatureField;
import com.ifttt.connect.api.UserFeatureStep;
import com.ifttt.connect.api.UserTokenProvider;
import com.ifttt.connect.ui.ButtonStateChangeListener;
import com.ifttt.connect.ui.ConnectButton;
import com.ifttt.connect.ui.ConnectButtonState;
import java.util.concurrent.TimeUnit;

/**
 * The main class for the Connect Location SDK. This class handles state change events from {@link ConnectButton},
 * sets up polling to get the latest location field values set for the connection, and
 * provides a function to add a listener to be notified when location permissions are required from the user
 */
public final class ConnectLocation implements ButtonStateChangeListener {

    /**
     * Callback interface used to listen to connection status change in {@link ConnectButton}.
     */
    public interface LocationPermissionCallback {

        /**
         * Called when a connection is enabled, it has an enabled {@link UserFeature} that uses at least one Location
         * trigger.
         */
        void onRequestLocationPermission();
    }

    private static ConnectLocation INSTANCE = null;
    final GeofenceProvider geofenceProvider;
    final ConnectionApiClient connectionApiClient;
    private final WorkManager workManager;

    private LocationPermissionCallback permissionCallback;

    final String connectionId;

    private final String WORK_ID_CONNECTION_POLLING = "connection_refresh_polling";
    private final long CONNECTION_REFRESH_POLLING_INTERVAL = 1;

    public static synchronized ConnectLocation init(
        Context context, String connectionId, ConnectionApiClient apiClient
    ) {
        if (INSTANCE == null) {
            INSTANCE = new ConnectLocation(connectionId,
                new AwarenessGeofenceProvider(context),
                apiClient,
                WorkManager.getInstance(context)
            );
        }
        return INSTANCE;
    }

    public static synchronized ConnectLocation init(
        Context context, String connectionId, UserTokenProvider userTokenProvider
    ) {
        if (INSTANCE == null) {
            ConnectionApiClient client = new ConnectionApiClient.Builder(context, userTokenProvider).build();
            INSTANCE = new ConnectLocation(connectionId,
                new AwarenessGeofenceProvider(context),
                client,
                WorkManager.getInstance(context)
            );
        }
        return INSTANCE;
    }

    public static ConnectLocation getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Connect Location is not initialized");
        }
        return INSTANCE;
    }

    /**
     * This is a required method to provide the Location SDK with a listener for state change events from {@link ConnectButton}.
     * The Connect Location SDK will not be able to handle state change events and register/unregister geofences
     * if you don't call this method.
     *
     * @param connectButton button instance initialized to display the connection.
     * @param permissionCallback {@link LocationPermissionCallback to be registered}. you will be notified when the
     * connection is enabled and location permissions need to be granted.
     */
    public void setUpWithConnectButton(ConnectButton connectButton, LocationPermissionCallback permissionCallback) {
        this.permissionCallback = permissionCallback;
        connectButton.addButtonStateChangeListener(this);
    }

    @VisibleForTesting
    ConnectLocation(
        String connectionId,
        GeofenceProvider geofenceProvider,
        ConnectionApiClient connectionApiClient,
        WorkManager workManager
    ) {
        this.connectionId = connectionId;
        this.geofenceProvider = geofenceProvider;
        this.connectionApiClient = connectionApiClient;
        this.workManager = workManager;

        setUpPolling();
    }

    private void setUpPolling() {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        workManager.enqueueUniquePeriodicWork(WORK_ID_CONNECTION_POLLING,
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(ConnectionRefresher.class,
                CONNECTION_REFRESH_POLLING_INTERVAL,
                TimeUnit.HOURS
            ).setConstraints(constraints).build()
        );
    }

    @Override
    public void onStateChanged(
        ConnectButtonState currentState, ConnectButtonState previousState, Connection connection
    ) {
        if (currentState == ConnectButtonState.Enabled
            || currentState == ConnectButtonState.Disabled
            || currentState == ConnectButtonState.Initial) {
            geofenceProvider.updateGeofences(connection);
        }

        if (hasEnabledLocationUserFeature(connection) && permissionCallback != null) {
            permissionCallback.onRequestLocationPermission();
        }
    }

    @Override
    public void onError(ErrorResponse errorResponse) {
        // No-op
    }

    @CheckResult
    private static boolean hasEnabledLocationUserFeature(Connection connection) {
        if (connection.status != Connection.Status.enabled) {
            return false;
        }

        for (Feature feature : connection.features) {
            if (feature.userFeatures == null) {
                continue;
            }

            for (UserFeature userFeature : feature.userFeatures) {
                if (!userFeature.enabled) {
                    continue;
                }

                for (UserFeatureStep step : userFeature.userFeatureSteps) {
                    for (UserFeatureField field : step.fields) {
                        if (GeofenceProvider.LOCATION_FIELD_TYPES_LIST.contains(field.fieldType)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
