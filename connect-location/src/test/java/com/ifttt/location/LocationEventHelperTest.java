package com.ifttt.location;

import static com.google.common.truth.Truth.assertThat;
import static com.ifttt.location.LocationEventAttributes.LOCATION_EVENT_DELAY_TO_COMPLETE;
import static com.ifttt.location.LocationEventAttributes.LOCATION_EVENT_DELAY_TO_UPLOAD;
import static com.ifttt.location.LocationEventAttributes.LOCATION_EVENT_ERROR_MESSAGE;
import static com.ifttt.location.LocationEventAttributes.LOCATION_EVENT_ERROR_TYPE;
import static com.ifttt.location.LocationEventAttributes.LOCATION_EVENT_EVENT_TYPE;
import static com.ifttt.location.LocationEventAttributes.LOCATION_EVENT_JOB_ID;
import static com.ifttt.location.LocationEventAttributes.LOCATION_EVENT_SOURCE;
import static com.ifttt.location.LocationEventAttributes.LocationDataSource.Awareness;
import static com.ifttt.location.LocationEventUploader.EventType.Entry;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.ConnectionApiClient;
import com.ifttt.connect.api.UserTokenProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class LocationEventHelperTest {

    private ConnectLocation connectLocation;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        connectLocation = new ConnectLocation(new GeofenceProvider() {
            @Override
            public void updateGeofences(
                Connection connection, @Nullable ConnectLocation.LocationStatusCallback locationStatusCallback
            ) {

            }

            @Override
            public void removeGeofences(@Nullable ConnectLocation.LocationStatusCallback locationStatusCallback) {

            }
        }, new ConnectionApiClient.Builder(context, new UserTokenProvider() {
            @Nullable
            @Override
            public String getUserToken() {
                return null;
            }
        }).build());
    }

    @Test
    public void logEventReportedShouldIncludeTypeAndSource() {
        AtomicReference<LocationEventType> typeRef = new AtomicReference<>();
        AtomicReference<Map<String, String>> dataRef = new AtomicReference<>();
        connectLocation.setLocationEventListener((type, data) -> {
            typeRef.set(type);
            dataRef.set(data);
        });

        LocationEventHelper.logEventReported(connectLocation, Entry, Awareness);
        assertThat(typeRef.get()).isEqualTo(LocationEventType.EventReported);
        assertThat(dataRef.get()).hasSize(2);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_EVENT_TYPE);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_SOURCE);
    }

    @Test
    public void logEventUploadAttemptedShouldIncludeProperEntries() {
        AtomicReference<LocationEventType> typeRef = new AtomicReference<>();
        AtomicReference<Map<String, String>> dataRef = new AtomicReference<>();
        connectLocation.setLocationEventListener((type, data) -> {
            typeRef.set(type);
            dataRef.set(data);
        });

        LocationEventHelper.logEventUploadAttempted(connectLocation,
            Entry,
            Awareness,
            "id",
            System.currentTimeMillis() - 1000
        );
        assertThat(typeRef.get()).isEqualTo(LocationEventType.EventUploadAttempted);
        assertThat(dataRef.get()).hasSize(4);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_EVENT_TYPE);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_SOURCE);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_JOB_ID);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_DELAY_TO_UPLOAD);
        assertThat(Long.valueOf(dataRef.get().get(LOCATION_EVENT_DELAY_TO_UPLOAD))).isGreaterThan(0);
    }

    @Test
    public void logEventUploadSuccessfulShouldIncludeProperEntries() {
        AtomicReference<LocationEventType> typeRef = new AtomicReference<>();
        AtomicReference<Map<String, String>> dataRef = new AtomicReference<>();
        connectLocation.setLocationEventListener((type, data) -> {
            typeRef.set(type);
            dataRef.set(data);
        });

        LocationEventHelper.logEventUploadSuccessful(connectLocation,
            Entry,
            Awareness,
            "id",
            System.currentTimeMillis() - 1000
        );
        assertThat(typeRef.get()).isEqualTo(LocationEventType.EventUploadSuccessful);
        assertThat(dataRef.get()).hasSize(4);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_EVENT_TYPE);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_SOURCE);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_JOB_ID);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_DELAY_TO_COMPLETE);
        assertThat(Long.valueOf(dataRef.get().get(LOCATION_EVENT_DELAY_TO_COMPLETE))).isGreaterThan(0);
    }

    @Test
    public void logEventUploadFailedShouldIncludeProperEntries() {
        AtomicReference<LocationEventType> typeRef = new AtomicReference<>();
        AtomicReference<Map<String, String>> dataRef = new AtomicReference<>();
        connectLocation.setLocationEventListener((type, data) -> {
            typeRef.set(type);
            dataRef.set(data);
        });

        LocationEventHelper.logEventUploadFailed(connectLocation,
            Entry,
            Awareness,
            "id",
            LocationEventAttributes.ErrorType.Network,
            "Network Error"
        );
        assertThat(typeRef.get()).isEqualTo(LocationEventType.EventUploadFailed);
        assertThat(dataRef.get()).hasSize(5);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_EVENT_TYPE);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_SOURCE);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_JOB_ID);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_ERROR_TYPE);
        assertThat(dataRef.get()).containsKey(LOCATION_EVENT_ERROR_MESSAGE);
    }
}
