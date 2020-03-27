package com.ifttt.location;

import androidx.annotation.Nullable;
import com.ifttt.connect.LocationFieldValue;

final class LocationTriggerField {

    @Nullable final String id;
    final String permissionName;
    final LocationFieldValue locationFieldValue;

    LocationTriggerField(@Nullable String id, String permissionName, LocationFieldValue locationFieldValue) {
        this.id = id;
        this.permissionName = permissionName;
        this.locationFieldValue = locationFieldValue;
    }
}
