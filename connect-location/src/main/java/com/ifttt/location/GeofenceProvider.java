package com.ifttt.location;

import android.Manifest;
import androidx.annotation.RequiresPermission;
import com.ifttt.connect.api.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract type for a geofence functionality provider. The type is responsbile for setting up geofences given a
 * {@link Connection}.
 */
interface GeofenceProvider {

    String FIELD_TYPE_LOCATION_ENTER = "LOCATION_ENTER";
    String FIELD_TYPE_LOCATION_EXIT = "LOCATION_EXIT";
    String FIELD_TYPE_LOCATION_ENTER_EXIT = "LOCATION_ENTER_OR_EXIT";

    Set<String> LOCATION_FIELD_TYPES_LIST = new HashSet<>(Arrays.asList(FIELD_TYPE_LOCATION_ENTER,
        FIELD_TYPE_LOCATION_EXIT,
        FIELD_TYPE_LOCATION_ENTER_EXIT
    ));

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void updateGeofences(final Connection connection);
}
