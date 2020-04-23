package com.ifttt.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;
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
import com.ifttt.connect.api.PendingResult;
import com.ifttt.connect.api.UserFeature;
import com.ifttt.connect.api.UserFeatureField;
import com.ifttt.connect.api.UserFeatureStep;
import com.ifttt.connect.api.UserTokenProvider;
import com.ifttt.connect.ui.ConnectButton;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

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

    private static final String WORK_ID_CONNECTION_POLLING = "connection_refresh_polling";
    private static final long CONNECTION_REFRESH_POLLING_INTERVAL = 1;

    private static ConnectLocation INSTANCE = null;

    final GeofenceProvider geofenceProvider;
    final ConnectionApiClient connectionApiClient;
    final String connectionId;

    @Nullable WeakReference<Connection> connectionWeakReference;

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
        checkInit(INSTANCE);

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
        connectButton.addButtonStateChangeListener(new LocationButtonStateChangeListener(connectButton.getContext(),
            permissionCallback
        ));
    }

    /**
     * Given the connection id passed in during initialization, fetch the connection data, and check if it has an
     * enabled {@link UserFeature} that uses location.
     *
     * This method should be used in addition to the {@link #init(Context, String, ConnectionApiClient)} or
     * {@link #init(Context, String, UserTokenProvider)} method in the initialization process, in order to set up
     * ConnectLocation to account for users who have the connection already enabled but hasn't had it set up in your
     * app. This method fetches the latest connection data, checks if, for the given user (via {@link UserTokenProvider},
     * there is an enabled {@link UserFeature} that uses Location trigger. If that is true, it will set up the geofences
     * and call the {@link LocationPermissionCallback#onRequestLocationPermission()}.
     *
     * If you have a connection that uses Location service, you should call this method in the first Activity that your
     * users use your app, so that you can prompt the location permission request as soon as possible.
     *
     * @param activity Activity instance of the place where the permission check happens.
     * @param permissionCallback Nullable {@link LocationPermissionCallback} instance, if non-null, it will be invoked
     * when the connection is enabled, has an enabled UserFeature that has at least one Location service trigger, and
     * the app doesn't have ACCESS_FINE_LOCATION permission.
     *
     * @return a {@link PendingResult} instance if we need to fetch the connection data, or null if we can reuse the
     * cached data.
     */
    @Nullable
    public PendingResult<Connection> checkLocationPermission(
        Activity activity, @Nullable LocationPermissionCallback permissionCallback
    ) {
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

        // Set up polling job for fetching the latest connection data.
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        workManager.enqueueUniquePeriodicWork(WORK_ID_CONNECTION_POLLING,
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(ConnectionRefresher.class,
                CONNECTION_REFRESH_POLLING_INTERVAL,
                TimeUnit.HOURS
            ).setConstraints(constraints).build()
        );
    }

    @CheckResult
    static boolean hasEnabledLocationUserFeature(Connection connection) {
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

    private static void checkInit(ConnectLocation connectLocation) {
        if (connectLocation.connectionApiClient != null
            && connectLocation.geofenceProvider != null
            && connectLocation.connectionId != null) {
            return;
        }

        throw new IllegalStateException(
            "ConnectLocation is not initialized correctly. Please call ConnectLocation.init first.");
    }
}
