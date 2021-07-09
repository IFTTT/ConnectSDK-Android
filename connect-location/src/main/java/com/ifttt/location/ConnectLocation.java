package com.ifttt.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.work.Worker;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.ConnectionApiClient;
import com.ifttt.connect.api.ErrorResponse;
import com.ifttt.connect.api.PendingResult;
import com.ifttt.connect.api.UserFeature;
import com.ifttt.connect.api.UserTokenProvider;
import com.ifttt.connect.ui.ButtonStateChangeListener;
import com.ifttt.connect.ui.ConnectButton;
import com.ifttt.connect.ui.ConnectButtonState;

import static androidx.core.content.ContextCompat.checkSelfPermission;
import static com.ifttt.connect.ui.ConnectButtonState.Disabled;
import static com.ifttt.connect.ui.ConnectButtonState.Enabled;

/**
 * The main class for the Connect Location SDK. This class handles state change events from {@link ConnectButton},
 * sets up polling to get the latest location field values set for the connection, and
 * provides a function to add a listener to be notified when location permissions are required from the user
 */
public final class ConnectLocation {

    /**
     * Callback interface used to listen to connection status change in {@link ConnectButton}.
     */
    public interface LocationStatusCallback {

        /**
         * Called when a connection is enabled, it has an enabled {@link UserFeature} that uses at least one Location
         * trigger.
         */
        void onRequestLocationPermission();

        /**
         * Called when a connection's corresponding geo-fences' status has changed.
         *
         * @param activated True if at least one geo-fence is currently active, false otherwise.
         */
        void onLocationStatusUpdated(boolean activated);
    }

    private static ConnectLocation INSTANCE = null;

    final GeofenceProvider geofenceProvider;
    final ConnectionApiClient connectionApiClient;

    public static synchronized ConnectLocation init(Context context, ConnectionApiClient apiClient) {
        ConnectionApiClient.Builder builder = apiClient.newBuilder(new CacheUserTokenProvider(
            new SharedPreferenceUserTokenCache(context),
            apiClient.userTokenProvider
        ));
        INSTANCE = new ConnectLocation(new AwarenessGeofenceProvider(context.getApplicationContext()), builder.build());
        return INSTANCE;
    }

    public static synchronized ConnectLocation init(Context context, UserTokenProvider userTokenProvider) {
        ConnectionApiClient client = new ConnectionApiClient.Builder(context,
            new CacheUserTokenProvider(new SharedPreferenceUserTokenCache(context), userTokenProvider)
        ).build();
        INSTANCE = new ConnectLocation(new AwarenessGeofenceProvider(context.getApplicationContext()), client);

        return INSTANCE;
    }

    public static synchronized ConnectLocation init(Context context) {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        ConnectionApiClient client = new ConnectionApiClient.Builder(context,
            new CacheUserTokenProvider(new SharedPreferenceUserTokenCache(context), null)
        ).build();
        INSTANCE = new ConnectLocation(new AwarenessGeofenceProvider(context.getApplicationContext()), client);

        return INSTANCE;
    }

    public static ConnectLocation getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                "ConnectLocation is not initialized correctly. Please call ConnectLocation.init first.");
        }

        return INSTANCE;
    }

    static boolean isInitialized() {
        return INSTANCE != null;
    }

    /**
     * This is a required method to provide the Location SDK with a listener for state change events from {@link ConnectButton}.
     * The Connect Location SDK will not be able to handle state change events and register/unregister geofences
     * if you don't call this method.
     *
     * @param connectButton button instance initialized to display the connection.
     * @param permissionCallback {@link LocationStatusCallback to be registered}. you will be notified when the
     * connection is enabled and location permissions need to be granted.
     */
    public void setUpWithConnectButton(ConnectButton connectButton, LocationStatusCallback permissionCallback) {
        connectButton.addButtonStateChangeListener(new ButtonStateChangeListener() {
            @Override
            public void onStateChanged(
                ConnectButtonState currentState, ConnectButtonState previousState, Connection connection
            ) {
                if (currentState == Enabled || currentState == Disabled) {
                    ConnectionRefresher.schedule(connectButton.getContext(), connection.id);
                }

                if (currentState == Enabled) {
                    doActivate(connectButton.getContext(), connection, permissionCallback);
                } else if (currentState == Disabled || currentState == ConnectButtonState.Initial) {
                    deactivate(connectButton.getContext(), permissionCallback);
                }
            }

            @Override
            public void onError(ErrorResponse errorResponse) {
                // No-op.
            }
        });
    }

    /**
     * Call this method with true/false to enable/disable logging.
     * The Logger doesn't differentiate between release and debug build variants, so call this method accordingly.
     * By default, logging is disabled, unless set explicitly by calling this method with true earlier.
     */
    public void setLoggingEnabled(Boolean enabled) {
        Logger.setLoggingEnabled(enabled);
    }

    /**
     * Given the connection id passed in during initialization, fetch the connection data, and check if it has an
     * enabled {@link UserFeature} that uses location.
     * <p>
     * This method should be used in addition to the {@link #init(Context, ConnectionApiClient)} or
     * {@link #init(Context, UserTokenProvider)} method in the initialization process, in order to set up
     * ConnectLocation to account for users who have the connection already enabled but hasn't had it set up in your
     * app. This method fetches the latest connection data, checks if, for the given user (via {@link UserTokenProvider},
     * there is an enabled {@link UserFeature} that uses Location trigger. If that is true, it will set up the geofences
     * and call the {@link LocationStatusCallback#onRequestLocationPermission()}.
     * <p>
     * If you have a connection that uses Location service, you should call this method in the first Activity that your
     * users use your app, so that you can prompt the location permission request as soon as possible.
     *
     * @param context Context instance of the place where the permission check happens.
     * @param connectionId Connection ID to be activated.
     * @param locationStatusCallback Nullable {@link LocationStatusCallback} instance, can be used to listen to geofence
     * status changes and location permission requests.
     * @return a {@link PendingResult} instance if we need to fetch the connection data, or null if we can reuse the
     * cached data.
     */
    @Nullable
    public PendingResult<Connection> activate(
        Context context, String connectionId, @Nullable LocationStatusCallback locationStatusCallback
    ) {
        // Set up polling job for fetching the latest connection data.
        ConnectionRefresher.schedule(context, connectionId);

        PendingResult<Connection> pendingResult = connectionApiClient.api().showConnection(connectionId);
        pendingResult.execute(new PendingResult.ResultCallback<Connection>() {
            @Override
            public void onSuccess(Connection result) {
                Logger.log("Connection " + connectionId + " fetched successfully");
                doActivate(context, result, locationStatusCallback);
            }

            @Override
            public void onFailure(ErrorResponse errorResponse) {
                Logger.error("Connection " + connectionId + " fetch failed with error response: " + errorResponse);
            }
        });

        return pendingResult;
    }

    /**
     * Remove all registered geo-fences and cancel polling {@link Worker}.
     */
    public void deactivate(Context context, @Nullable LocationStatusCallback locationStatusCallback) {
        Logger.log("Deactivating geo-fence");
        geofenceProvider.removeGeofences(locationStatusCallback);
        ConnectionRefresher.cancel(context);

        new SharedPreferenceUserTokenCache(context).clear();
    }

    /**
     * Report geofencing events from external sources. This can be used as a supplement data point
     * to the internal SDK's geofencing logic.
     * <p>
     * If you have set up geo-location monitoring differently, and can report whether the device
     * has entered/exited a geofence, you can use this method to report it to IFTTT. This feature
     * includes mechanism to prevent the same geofence event being reported here as well as from
     * the SDK.
     *
     * @param context Context object.
     */
    public void reportEvent(
        Context context, double lat, double lng, @Nullable OnEventUploadListener listener
    ) {
        BackupGeofenceMonitor monitor = BackupGeofenceMonitor.get(context);
        monitor.checkMonitoredGeofences(lat, lng, new OnEventUploadListener() {
            @Override
            public void onUploadEvent(String fenceKey, LocationEventUploader.EventType eventType) {
                if (listener != null) {
                    listener.onUploadEvent(fenceKey, eventType);
                }

                String stepId = LocationEventUploadHelper.extractStepId(fenceKey);
                LocationEventUploader.schedule(context, eventType, stepId);
                Logger.log(eventType + " event reported, uploading with fence key: " + fenceKey);
            }

            @Override
            public void onUploadSkipped(String fenceKey, String reason) {
                if (listener != null) {
                    listener.onUploadSkipped(fenceKey, reason);
                }
            }
        });
    }

    @VisibleForTesting
    ConnectLocation(GeofenceProvider geofenceProvider, ConnectionApiClient connectionApiClient) {
        this.geofenceProvider = geofenceProvider;
        this.connectionApiClient = connectionApiClient;
    }

    private void doActivate(
        Context context, Connection connection, @Nullable LocationStatusCallback locationStatusCallback
    ) {
        boolean hasLocationTrigger = !LocationEventUploadHelper.extractLocationUserFeatures(connection.features, false)
            .isEmpty();
        if (!hasLocationTrigger) {
            // No-op if there is no enabled location trigger.
            return;
        }

        if (checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            geofenceProvider.updateGeofences(connection, locationStatusCallback);
        } else if (locationStatusCallback != null) {
            boolean hasEnabledLocationTrigger
                = !LocationEventUploadHelper.extractLocationUserFeatures(connection.features, true).isEmpty();
            if (!hasEnabledLocationTrigger) {
                // No-op if there is no enabled location trigger.
                return;
            }

            Logger.warning("ACCESS_FINE_LOCATION permission not granted");
            locationStatusCallback.onRequestLocationPermission();
        }
    }
}
