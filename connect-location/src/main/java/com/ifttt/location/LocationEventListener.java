package com.ifttt.location;

import java.util.Map;

/**
 * Listener interface that reports background location events and their attributes
 * whenever a location change happens on any of the registered geofences.
 */
public interface LocationEventListener {

    void onLocationEventReported(LocationEventType type, Map<String, String> data);
}
