package com.ifttt.location;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.common.collect.ImmutableList;
import com.ifttt.connect.api.Feature;
import com.ifttt.connect.api.LocationFieldValue;
import com.ifttt.connect.api.UserFeature;
import com.ifttt.connect.api.UserFeatureField;
import com.ifttt.connect.api.UserFeatureStep;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.truth.Truth.assertThat;
import static com.ifttt.connect.api.FeatureStep.StepType.Trigger;
import static com.ifttt.location.BackupGeofenceMonitor.MonitoredGeofence.GeofenceState.Entered;
import static com.ifttt.location.BackupGeofenceMonitor.MonitoredGeofence.GeofenceState.Exited;
import static com.ifttt.location.BackupGeofenceMonitor.MonitoredGeofence.GeofenceState.Init;
import static com.ifttt.location.GeofenceProvider.FIELD_TYPE_LOCATION_ENTER;
import static com.ifttt.location.GeofenceProvider.FIELD_TYPE_LOCATION_ENTER_EXIT;
import static com.ifttt.location.GeofenceProvider.FIELD_TYPE_LOCATION_EXIT;
import static com.ifttt.location.LocationEventUploader.EventType.Entry;
import static com.ifttt.location.LocationEventUploader.EventType.Exit;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public final class BackupGeofenceMonitorTest {

    private static final String PREFS_GEOFENCE_MONITOR = "ifttt_geofence_monitor";
    private static final String PREF_KEY_MONITORED_GEOFENCES = "monitored_geofences";

    private SharedPreferences preferences;
    private JsonAdapter<Map<String, BackupGeofenceMonitor.MonitoredGeofence>> jsonAdapter;

    @Before
    public void setUp() {
        preferences = ApplicationProvider.getApplicationContext().getSharedPreferences(
            PREFS_GEOFENCE_MONITOR,
            Context.MODE_PRIVATE
        );
        jsonAdapter = new Moshi.Builder().build().adapter(Types.newParameterizedType(
            Map.class,
            String.class,
            BackupGeofenceMonitor.MonitoredGeofence.class
        ));
    }

    @Test
    public void cacheShouldRegisterEntry() throws IOException {
        BackupGeofenceMonitor monitor = BackupGeofenceMonitor.get(ApplicationProvider.getApplicationContext());
        updateMonitorCache(monitor, FIELD_TYPE_LOCATION_ENTER, "id");

        Map<String, BackupGeofenceMonitor.MonitoredGeofence> cache = cache();
        assertThat(cache).hasSize(1);
        assertThat(cache.get("ifttt_id").state).isEqualTo(Init);
        assertThat(cache.get("ifttt_id").type).isEqualTo(Entry);
    }

    @Test
    public void cacheShouldRegisterExit() throws IOException {
        BackupGeofenceMonitor monitor = BackupGeofenceMonitor.get(ApplicationProvider.getApplicationContext());
        updateMonitorCache(monitor, FIELD_TYPE_LOCATION_EXIT, "id");

        Map<String, BackupGeofenceMonitor.MonitoredGeofence> cache = cache();
        assertThat(cache).hasSize(1);
        assertThat(cache.get("ifttt_id").state).isEqualTo(Init);
        assertThat(cache.get("ifttt_id").type).isEqualTo(Exit);
    }

    @Test
    public void cacheShouldRegisterBoth() throws IOException {
        BackupGeofenceMonitor monitor = BackupGeofenceMonitor.get(ApplicationProvider.getApplicationContext());
        updateMonitorCache(monitor, FIELD_TYPE_LOCATION_ENTER_EXIT, "id");

        Map<String, BackupGeofenceMonitor.MonitoredGeofence> cache = cache();
        assertThat(cache).hasSize(2);
        assertThat(cache.get("ifttt_id/enter").state).isEqualTo(Init);
        assertThat(cache.get("ifttt_id/enter").type).isEqualTo(Entry);
        assertThat(cache.get("ifttt_id/exit").state).isEqualTo(Init);
        assertThat(cache.get("ifttt_id/exit").type).isEqualTo(Exit);
    }

    @Test
    public void cacheShouldOverride() throws IOException {
        BackupGeofenceMonitor monitor = BackupGeofenceMonitor.get(ApplicationProvider.getApplicationContext());
        updateMonitorCache(monitor, FIELD_TYPE_LOCATION_EXIT, "id_old");
        updateMonitorCache(monitor, FIELD_TYPE_LOCATION_EXIT, "id_new");

        Map<String, BackupGeofenceMonitor.MonitoredGeofence> cache = cache();
        assertThat(cache).hasSize(1);
        assertThat(cache).doesNotContainKey("ifttt_id_old");
        assertThat(cache.get("ifttt_id_new").state).isEqualTo(Init);
        assertThat(cache.get("ifttt_id_new").type).isEqualTo(Exit);
    }

    @Test
    public void cacheShouldMaintainExisting() throws IOException {
        BackupGeofenceMonitor monitor = BackupGeofenceMonitor.get(ApplicationProvider.getApplicationContext());

        LocationFieldValue locationFieldValue = new LocationFieldValue(0.0, 0.0, 100.0, "");
        UserFeatureField<LocationFieldValue> locationFieldValueUserFeatureField = new UserFeatureField<>(
            locationFieldValue,
            FIELD_TYPE_LOCATION_EXIT,
            "id"
        );
        Feature feature1 = new Feature(
            "id",
            "title",
            "description",
            "iconUrl",
            ImmutableList.of(new UserFeature("id",
                "featureId",
                true,
                ImmutableList.of(new UserFeatureStep(Trigger,
                    "id_1",
                    "step_id",
                    ImmutableList.of(locationFieldValueUserFeatureField)
                ))
            ))
        );
        Feature feature2 = new Feature(
            "id",
            "title",
            "description",
            "iconUrl",
            ImmutableList.of(new UserFeature("id",
                "featureId",
                true,
                ImmutableList.of(new UserFeatureStep(Trigger,
                    "id_2",
                    "step_id",
                    ImmutableList.of(locationFieldValueUserFeatureField)
                ))
            ))
        );
        monitor.updateMonitoredGeofences(ImmutableList.of(feature1, feature2));

        monitor.setState("ifttt_id_1", Exited);
        monitor.setState("ifttt_id_2", Exited);

        monitor.updateMonitoredGeofences(ImmutableList.of(feature1, feature2));

        Map<String, BackupGeofenceMonitor.MonitoredGeofence> cache = cache();
        assertThat(cache).hasSize(2);
        assertThat(cache.get("ifttt_id_1").state).isEqualTo(Exited);
        assertThat(cache.get("ifttt_id_2").state).isEqualTo(Exited);
    }

    @Test
    public void cacheShouldClear() throws IOException {
        BackupGeofenceMonitor monitor = BackupGeofenceMonitor.get(ApplicationProvider.getApplicationContext());
        updateMonitorCache(monitor, FIELD_TYPE_LOCATION_EXIT, "id");
        monitor.updateMonitoredGeofences(Collections.emptyList());

        Map<String, BackupGeofenceMonitor.MonitoredGeofence> cache = cache();
        assertThat(cache).isEmpty();
    }

    @Test
    public void checkMonitoredGeofencesShouldCallListenerAfterInit() {
        BackupGeofenceMonitor monitor = BackupGeofenceMonitor.get(ApplicationProvider.getApplicationContext());
        updateMonitorCache(monitor, FIELD_TYPE_LOCATION_EXIT, "id");

        AtomicReference<LocationEventUploader.EventType> typeRef = new AtomicReference<>();
        AtomicReference<String> keyRef = new AtomicReference<>();
        monitor.checkMonitoredGeofences(0.5, 0.5, new OnEventUploadListener() {
            @Override
            public void onUploadEvent(String fenceKey, LocationEventUploader.EventType eventType) {
                keyRef.set(fenceKey);
                typeRef.set(eventType);
            }

            @Override
            public void onUploadSkipped(String fenceKey, String reason) {
                fail();
            }
        });

        assertThat(keyRef.get()).isEqualTo("ifttt_id");
        assertThat(typeRef.get()).isEqualTo(Exit);
    }

    @Test
    public void checkMonitoredGeofencesShouldCallListenerForDiff() {
        BackupGeofenceMonitor monitor = BackupGeofenceMonitor.get(ApplicationProvider.getApplicationContext());
        updateMonitorCache(monitor, FIELD_TYPE_LOCATION_EXIT, "id");

        String fenceKey = "ifttt_id";
        monitor.setState(fenceKey, Entered);

        AtomicReference<LocationEventUploader.EventType> typeRef = new AtomicReference<>();
        AtomicReference<String> keyRef = new AtomicReference<>();
        monitor.checkMonitoredGeofences(0.5, 0.5, new OnEventUploadListener() {
            @Override
            public void onUploadEvent(String fenceKey, LocationEventUploader.EventType eventType) {
                keyRef.set(fenceKey);
                typeRef.set(eventType);
            }

            @Override
            public void onUploadSkipped(String fenceKey, String reason) {
                fail();
            }
        });

        assertThat(keyRef.get()).isEqualTo(fenceKey);
        assertThat(typeRef.get()).isEqualTo(Exit);
    }

    @Test
    public void checkMonitoredGeofencesShouldNotCallListenerForDup() {
        BackupGeofenceMonitor monitor = BackupGeofenceMonitor.get(ApplicationProvider.getApplicationContext());
        updateMonitorCache(monitor, FIELD_TYPE_LOCATION_EXIT, "id");

        String fenceKey = "ifttt_id";
        monitor.setState(fenceKey, Exited);

        AtomicReference<LocationEventUploader.EventType> typeRef = new AtomicReference<>();
        AtomicReference<String> keyRef = new AtomicReference<>();

        monitor.checkMonitoredGeofences(0.5, 0.5, new OnEventUploadListener() {
            @Override
            public void onUploadEvent(String fenceKey, LocationEventUploader.EventType eventType) {
                keyRef.set(fenceKey);
                typeRef.set(eventType);
            }

            @Override
            public void onUploadSkipped(String fenceKey, String reason) {
                fail();
            }
        });

        assertThat(keyRef.get()).isNull();
        assertThat(typeRef.get()).isNull();
    }

    static void updateMonitorCache(BackupGeofenceMonitor monitor, String fieldType, String id) {
        LocationFieldValue locationFieldValue = new LocationFieldValue(0.0, 0.0, 100.0, "");
        UserFeatureField<LocationFieldValue> locationFieldValueUserFeatureField = new UserFeatureField<>(
            locationFieldValue,
            fieldType,
            id
        );
        monitor.updateMonitoredGeofences(ImmutableList.of(new Feature(
            "id",
            "title",
            "description",
            "iconUrl",
            ImmutableList.of(new UserFeature("id",
                "featureId",
                true,
                ImmutableList.of(new UserFeatureStep(Trigger,
                    id,
                    "step_id",
                    ImmutableList.of(locationFieldValueUserFeatureField)
                ))
            ))
        )));
    }

    private Map<String, BackupGeofenceMonitor.MonitoredGeofence> cache() throws IOException {
        return jsonAdapter.fromJson(preferences.getString(PREF_KEY_MONITORED_GEOFENCES, null));
    }
}
