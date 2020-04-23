package com.ifttt.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.ErrorResponse;
import com.ifttt.connect.ui.ButtonStateChangeListener;
import com.ifttt.connect.ui.ConnectButtonState;

import static androidx.core.content.ContextCompat.checkSelfPermission;
import static com.ifttt.location.ConnectLocation.hasEnabledLocationUserFeature;

/**
 * A {@link ButtonStateChangeListener} implementation that incorporates the button state changes with the location
 * geofence setup, as well as location permission requests.
 */
final class LocationButtonStateChangeListener implements ButtonStateChangeListener {

    private final ConnectLocation.LocationPermissionCallback locationPermissionCallback;
    private final Context context;

    LocationButtonStateChangeListener(
        Context context, ConnectLocation.LocationPermissionCallback locationPermissionCallback
    ) {
        this.context = context;
        this.locationPermissionCallback = locationPermissionCallback;
    }

    @Override
    public void onStateChanged(
        ConnectButtonState currentState, ConnectButtonState previousState, Connection connection
    ) {
        boolean hasLocationPermission = checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
        if (currentState == ConnectButtonState.Enabled
            || currentState == ConnectButtonState.Disabled
            || currentState == ConnectButtonState.Initial) {
            if (hasLocationPermission) {
                ConnectLocation.getInstance().geofenceProvider.updateGeofences(connection);
            }
        }

        boolean hasEnabledLocationTrigger = hasEnabledLocationUserFeature(connection);
        if (!hasLocationPermission && hasEnabledLocationTrigger) {
            locationPermissionCallback.onRequestLocationPermission();
        }
    }

    @Override
    public void onError(ErrorResponse errorResponse) {
        // No-op
    }
}
