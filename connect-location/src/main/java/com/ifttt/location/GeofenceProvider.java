package com.ifttt.location;

import com.ifttt.connect.Connection;

interface GeofenceProvider {

    void updateGeofences(final Connection connection);

}
