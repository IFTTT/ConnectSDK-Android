package com.ifttt.location;

import android.content.Context;
import android.content.SharedPreferences;
import com.ifttt.location.BackupGeofenceMonitor.MonitoredGeofence;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

final class SharedPreferencesGeofenceCache implements Cache<Map<String, MonitoredGeofence>> {

    private static final String PREFS_GEOFENCE_MONITOR = "ifttt_geofence_monitor";
    private static final String PREF_KEY_MONITORED_GEOFENCES = "monitored_geofences";
    private static final Moshi MOSHI_INSTANCE = new Moshi.Builder().build();

    private final SharedPreferences sharedPreferences;
    private final JsonAdapter<Map<String, MonitoredGeofence>> jsonAdapter;

    SharedPreferencesGeofenceCache(Context context) {
        jsonAdapter = MOSHI_INSTANCE.adapter(Types.newParameterizedType(
            Map.class,
            String.class,
            MonitoredGeofence.class
        ));
        sharedPreferences = context.getSharedPreferences(PREFS_GEOFENCE_MONITOR, Context.MODE_PRIVATE);
    }

    @Override
    public void write(Map<String, MonitoredGeofence> data) {
        String geofencesString = jsonAdapter.toJson(data);
        sharedPreferences.edit().putString(PREF_KEY_MONITORED_GEOFENCES, geofencesString).apply();
    }

    @Override
    public Map<String, MonitoredGeofence> read() {
        String monitoredGeofencesString = sharedPreferences.getString(PREF_KEY_MONITORED_GEOFENCES, null);
        if (monitoredGeofencesString == null) {
            return Collections.emptyMap();
        }

        try {
            return jsonAdapter.fromJson(monitoredGeofencesString);
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    @Override
    public void clear() {
        sharedPreferences.edit().remove(PREF_KEY_MONITORED_GEOFENCES).apply();
    }
}
