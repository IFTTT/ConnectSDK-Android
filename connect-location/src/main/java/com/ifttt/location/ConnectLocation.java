package com.ifttt.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.work.Worker;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.ConnectionApiClient;
import com.ifttt.connect.api.ErrorResponse;
import com.ifttt.connect.api.LocationFieldValue;
import com.ifttt.connect.api.PendingResult;
import com.ifttt.connect.api.UserFeature;
import com.ifttt.connect.api.UserFeatureField;
import com.ifttt.connect.api.UserTokenProvider;
import com.ifttt.connect.ui.ButtonStateChangeListener;
import com.ifttt.connect.ui.ConnectButton;
import com.ifttt.connect.ui.ConnectButtonState;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import static androidx.core.content.ContextCompat.checkSelfPermission;

/**
 * The main class for the Connect Location SDK. This class handles state change events from {@link ConnectButton},
 * sets up polling to get the latest location field values set for the connection, and
 * provides a function to add a listener to be notified when location permissions are required from the user
 */
public final class ConnectLocation {

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

    @Nullable WeakReference<Connection> connectionWeakReference;

    public static synchronized ConnectLocation init(Context context, ConnectionApiClient apiClient) {
        if (INSTANCE == null) {
            INSTANCE = new ConnectLocation(new AwarenessGeofenceProvider(context.getApplicationContext()), apiClient);
        }
        return INSTANCE;
    }

    public static synchronized ConnectLocation init(
        Context context, UserTokenProvider userTokenProvider
    ) {
        if (INSTANCE == null) {
            ConnectionApiClient client = new ConnectionApiClient.Builder(context, userTokenProvider).build();
            INSTANCE = new ConnectLocation(new AwarenessGeofenceProvider(context.getApplicationContext()), client);
        }
        return INSTANCE;
    }

    public static ConnectLocation getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                "ConnectLocation is not initialized correctly. Please call ConnectLocation.init first.");
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
        connectButton.addButtonStateChangeListener(new ButtonStateChangeListener() {
            @Override
            public void onStateChanged(
                ConnectButtonState currentState, ConnectButtonState previousState, Connection connection
            ) {
                boolean hasLocationPermission = checkSelfPermission(connectButton.getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                    == PackageManager.PERMISSION_GRANTED;
                if (currentState == ConnectButtonState.Enabled
                    || currentState == ConnectButtonState.Disabled
                    || currentState == ConnectButtonState.Initial) {
                    if (hasLocationPermission) {
                        geofenceProvider.updateGeofences(connection);
                    }
                }

                boolean hasEnabledLocationTrigger = hasEnabledLocationUserFeature(connection);
                if (!hasLocationPermission && hasEnabledLocationTrigger) {
                    connectionWeakReference = new WeakReference<>(connection);
                    permissionCallback.onRequestLocationPermission();
                }
            }

            @Override
            public void onError(ErrorResponse errorResponse) {
                // No-op.
            }
        });
    }

    /**
     * Given the connection id passed in during initialization, fetch the connection data, and check if it has an
     * enabled {@link UserFeature} that uses location.
     *
     * This method should be used in addition to the {@link #init(Context, ConnectionApiClient)} or
     * {@link #init(Context, UserTokenProvider)} method in the initialization process, in order to set up
     * ConnectLocation to account for users who have the connection already enabled but hasn't had it set up in your
     * app. This method fetches the latest connection data, checks if, for the given user (via {@link UserTokenProvider},
     * there is an enabled {@link UserFeature} that uses Location trigger. If that is true, it will set up the geofences
     * and call the {@link LocationPermissionCallback#onRequestLocationPermission()}.
     *
     * If you have a connection that uses Location service, you should call this method in the first Activity that your
     * users use your app, so that you can prompt the location permission request as soon as possible.
     *
     * @param activity Activity instance of the place where the permission check happens.
     * @param connectionId Connection ID to be activated.
     * @param permissionCallback Nullable {@link LocationPermissionCallback} instance, if non-null, it will be invoked
     * when the connection is enabled, has an enabled UserFeature that has at least one Location service trigger, and
     * the app doesn't have ACCESS_FINE_LOCATION permission.
     * @return a {@link PendingResult} instance if we need to fetch the connection data, or null if we can reuse the
     * cached data.
     */
    @Nullable
    public PendingResult<Connection> activate(
        Activity activity, String connectionId, @Nullable LocationPermissionCallback permissionCallback
    ) {
        // Set up polling job for fetching the latest connection data.
        ConnectionRefresher.schedule(activity, connectionId);

        Connection cachedConnection;
        if (connectionWeakReference != null && (cachedConnection = connectionWeakReference.get()) != null) {
            if (checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                geofenceProvider.updateGeofences(cachedConnection);
            } else if (hasEnabledLocationUserFeature(cachedConnection) && permissionCallback != null) {
                permissionCallback.onRequestLocationPermission();
            }

            return null;
        }

        PendingResult<Connection> pendingResult = connectionApiClient.api().showConnection(connectionId);
        pendingResult.execute(new PendingResult.ResultCallback<Connection>() {
            @Override
            public void onSuccess(Connection result) {
                connectionWeakReference = new WeakReference<>(result);
                boolean hasEnabledLocationTrigger = hasEnabledLocationUserFeature(result);

                if (checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    geofenceProvider.updateGeofences(result);
                } else if (hasEnabledLocationTrigger && permissionCallback != null) {
                    permissionCallback.onRequestLocationPermission();
                }
            }

            @Override
            public void onFailure(ErrorResponse errorResponse) {
                // No-op
            }
        });

        return pendingResult;
    }

    /**
     * Remove all registered geo-fences and cancel polling {@link Worker}.
     */
    public void deactivate(Context context) {
        geofenceProvider.removeGeofences();

        ConnectionRefresher.cancel(context);
    }

    @VisibleForTesting
    ConnectLocation(GeofenceProvider geofenceProvider, ConnectionApiClient connectionApiClient) {
        this.geofenceProvider = geofenceProvider;
        this.connectionApiClient = connectionApiClient;
    }

    @CheckResult
    private static boolean hasEnabledLocationUserFeature(Connection connection) {
        if (connection.status != Connection.Status.enabled) {
            return false;
        }

        Map<String, List<UserFeatureField<LocationFieldValue>>> result
            = LocationEventUploadHelper.extractLocationUserFeatures(connection.features);
        return !result.isEmpty();
    }
}
