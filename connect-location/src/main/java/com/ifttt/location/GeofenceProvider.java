package com.ifttt.location;

import com.ifttt.connect.Connection;

/**
 * Abstract type for a geofence functionality provider. The type is responsbile for setting up geofences given a
 * {@link Connection}.
 */
interface GeofenceProvider {

    void updateGeofences(final Connection connection);
}
