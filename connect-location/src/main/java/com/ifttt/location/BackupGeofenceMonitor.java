package com.ifttt.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import androidx.annotation.Nullable;
import com.ifttt.connect.api.Feature;
import com.ifttt.connect.api.LocationFieldValue;
import com.ifttt.connect.api.UserFeatureField;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ifttt.location.BackupGeofenceMonitor.MonitoredGeofence.GeofenceState.Entered;
import static com.ifttt.location.BackupGeofenceMonitor.MonitoredGeofence.GeofenceState.Exited;
import static com.ifttt.location.BackupGeofenceMonitor.MonitoredGeofence.GeofenceState.Init;
import static com.ifttt.location.GeofenceProvider.FIELD_TYPE_LOCATION_ENTER;
import static com.ifttt.location.GeofenceProvider.FIELD_TYPE_LOCATION_ENTER_EXIT;
import static com.ifttt.location.GeofenceProvider.FIELD_TYPE_LOCATION_EXIT;
import static com.ifttt.location.LocationEventUploadHelper.extractLocationUserFeatures;
import static com.ifttt.location.LocationEventUploadHelper.getEnterFenceKey;
import static com.ifttt.location.LocationEventUploadHelper.getExitFenceKey;
import static com.ifttt.location.LocationEventUploadHelper.getIftttFenceKey;
import static com.ifttt.location.LocationEventUploader.EventType.Entry;
import static com.ifttt.location.LocationEventUploader.EventType.Exit;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of a potential backup monitor to all geofences from a {@link com.ifttt.connect.api.Connection}.
 */
final class BackupGeofenceMonitor {

    static BackupGeofenceMonitor get(Context context) {
        return new BackupGeofenceMonitor(new SharedPreferencesGeofenceCache(context));
    }

    private final Cache<Map<String, MonitoredGeofence>> cache;

    private BackupGeofenceMonitor(Cache<Map<String, MonitoredGeofence>> cache) {
        this.cache = cache;
    }

    /**
     * Refresh the cached monitored geofences, given a list of {@link Feature} from a Connection.
     *
     * @param features Feature list from a Connection.
     */
    void updateMonitoredGeofences(List<Feature> features) {
        LinkedHashMap<String, MonitoredGeofence> map = new LinkedHashMap<>();
        Map<String, MonitoredGeofence> existingMap = getMonitoredGeofences();
        Map<String, List<UserFeatureField<LocationFieldValue>>> locations = extractLocationUserFeatures(features, true);
        for (Map.Entry<String, List<UserFeatureField<LocationFieldValue>>> entry : locations.entrySet()) {
            String id = getIftttFenceKey(entry.getKey());
            for (UserFeatureField<LocationFieldValue> userFeatureField : entry.getValue()) {
                switch (userFeatureField.fieldType) {
                    case FIELD_TYPE_LOCATION_ENTER:
                        if (existingMap.containsKey(id)) {
                            map.put(id, existingMap.get(id));
                        } else {
                            map.put(id, new MonitoredGeofence(
                                LocationEventUploader.EventType.Entry,
                                MonitoredGeofence.GeofenceState.Init,
                                userFeatureField.value
                            ));
                        }
                        break;
                    case FIELD_TYPE_LOCATION_EXIT:
                        if (existingMap.containsKey(id)) {
                            map.put(id, existingMap.get(id));
                        } else {
                            map.put(id, new MonitoredGeofence(
                                LocationEventUploader.EventType.Exit,
                                MonitoredGeofence.GeofenceState.Init,
                                userFeatureField.value
                            ));
                        }
                        break;
                    case FIELD_TYPE_LOCATION_ENTER_EXIT:
                        String enterKey = getEnterFenceKey(id);
                        String exitKey = getExitFenceKey(id);
                        if (existingMap.containsKey(enterKey)) {
                            map.put(id, existingMap.get(enterKey));
                        } else {
                            map.put(getEnterFenceKey(id), new MonitoredGeofence(
                                LocationEventUploader.EventType.Entry,
                                MonitoredGeofence.GeofenceState.Init,
                                userFeatureField.value
                            ));
                        }
                        if (existingMap.containsKey(exitKey)) {
                            map.put(id, existingMap.get(exitKey));
                        } else {
                            map.put(getExitFenceKey(id), new MonitoredGeofence(
                                LocationEventUploader.EventType.Exit,
                                MonitoredGeofence.GeofenceState.Init,
                                userFeatureField.value
                            ));
                        }
                        break;
                    default:
                        // No-op for other location types.
                }
            }
        }

        if (!map.isEmpty()) {
            cache.write(map);
        }
    }

    void clear() {
        cache.clear();
    }

    /**
     * Refresh the {@link MonitoredGeofence.GeofenceState} of a single geofence in the cached
     * monitored geofences.
     *
     * @param fenceKey Geofence identifier, must match one of the geofences registered internally in
     * the SDK.
     * @param state New {@link MonitoredGeofence.GeofenceState} for the fence key.
     */
    void setState(String fenceKey, MonitoredGeofence.GeofenceState state) {
        Map<String, MonitoredGeofence> map = getMonitoredGeofences();
        MonitoredGeofence currentGeofence = map.get(fenceKey);
        if (currentGeofence == null) {
            return;
        }

        map.put(fenceKey, new MonitoredGeofence(currentGeofence.type, state, currentGeofence.value));
        cache.write(map);
    }

    /**
     * Read the cached monitored geofence given the fence key.
     *
     * @param fenceKey Geofence identifier, must match one of the geofences registered internally in
     * the SDK.
     * @return cached {@link MonitoredGeofence.GeofenceState} for the fence key, or null if state
     * cannot be found.
     */
    @Nullable
    MonitoredGeofence.GeofenceState getState(String fenceKey) {
        Map<String, MonitoredGeofence> map = getMonitoredGeofences();
        MonitoredGeofence currentGeofence = map.get(fenceKey);
        return currentGeofence != null ? currentGeofence.state : null;
    }

    /**
     * @return all monitored geofence states.
     */
    Map<String, MonitoredGeofence> getMonitoredGeofences() {
        return cache.read();
    }

    void checkMonitoredGeofences(double lat, double lng, OnEventUploadListener listener) {
        Map<String, MonitoredGeofence> monitoredGeofences = getMonitoredGeofences();
        for (Map.Entry<String, MonitoredGeofence> entry : monitoredGeofences.entrySet()) {
            MonitoredGeofence monitoredGeofence = entry.getValue();
            requireNonNull(monitoredGeofence.value.radius);

            Location newLocation = new Location(LocationManager.PASSIVE_PROVIDER);
            newLocation.setLatitude(lat);
            newLocation.setLongitude(lng);

            Location geofence = new Location(LocationManager.PASSIVE_PROVIDER);
            geofence.setLatitude(monitoredGeofence.value.lat);
            geofence.setLongitude(monitoredGeofence.value.lng);

            MonitoredGeofence.GeofenceState newState;
            LocationEventUploader.EventType newType;
            if (newLocation.distanceTo(geofence) >= monitoredGeofence.value.radius) {
                newState = Exited;
                newType = Exit;
            } else {
                newState = Entered;
                newType = Entry;
            }

            boolean isMatchingEntryEvent = newState == Entered && monitoredGeofence.type == Entry;
            boolean isMatchingExitEvent = newState == Exited && monitoredGeofence.type == Exit;

            if (newState != monitoredGeofence.state) {
                if (monitoredGeofence.state != Init && (isMatchingEntryEvent || isMatchingExitEvent)) {
                    listener.onUploadEvent(entry.getKey(), newType);
                } else {
                    // In this case, the enter/exit event has already been reported, most likely
                    // from Awareness API.
                    String fenceKey = entry.getKey();
                    listener.onUploadSkipped(
                        fenceKey,
                        "Upload is skipped, with state: "
                            + monitoredGeofence.state
                            + ", type: "
                            + monitoredGeofence.type
                            + ", event: "
                            + newState
                    );
                }

                setState(entry.getKey(), newState);
            }
        }
    }

    static final class MonitoredGeofence {

        enum GeofenceState {
            Entered, Exited, Init
        }

        final LocationEventUploader.EventType type;
        final GeofenceState state;
        final LocationFieldValue value;

        MonitoredGeofence(
            LocationEventUploader.EventType type, GeofenceState state, LocationFieldValue value
        ) {
            this.type = type;
            this.state = state;
            this.value = value;
        }
    }
}
