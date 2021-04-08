package com.ifttt.location;

import com.ifttt.location.BackupGeofenceMonitor.MonitoredGeofence;
import java.util.Map;

interface GeofenceCache {

    void write(Map<String, MonitoredGeofence> data);

    Map<String, MonitoredGeofence> read();
}
