package com.ifttt.location;

import androidx.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.ifttt.location.LocationEventType.EventReported;
import static com.ifttt.location.LocationEventType.EventUploadAttempted;
import static java.util.Objects.requireNonNull;

final class LocationEventHelper {

    static void logEventReported(
        ConnectLocation connectLocation,
        LocationEventUploader.EventType eventType,
        LocationEventAttributes.LocationDataSource source
    ) {
        if (connectLocation.locationEventListener == null) {
            return;
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put(LocationEventAttributes.LOCATION_EVENT_EVENT_TYPE, eventType.name());
        data.put(LocationEventAttributes.LOCATION_EVENT_SOURCE, source.name());
        requireNonNull(connectLocation.locationEventListener).onLocationEventReported(EventReported, data);
    }

    static void logEventUploadAttempted(
        ConnectLocation connectLocation,
        LocationEventUploader.EventType eventType,
        LocationEventAttributes.LocationDataSource source,
        @Nullable String jobId,
        long scheduledTimestamp
    ) {
        if (connectLocation.locationEventListener == null) {
            return;
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put(LocationEventAttributes.LOCATION_EVENT_EVENT_TYPE, eventType.name());
        data.put(LocationEventAttributes.LOCATION_EVENT_JOB_ID, jobId);
        data.put(LocationEventAttributes.LOCATION_EVENT_DELAY_TO_UPLOAD,
            String.valueOf(System.currentTimeMillis() - scheduledTimestamp)
        );
        data.put(LocationEventAttributes.LOCATION_EVENT_SOURCE, source.name());
        connectLocation.locationEventListener.onLocationEventReported(EventUploadAttempted, data);
    }

    static void logEventUploadSuccessful(
        ConnectLocation connectLocation,
        LocationEventUploader.EventType eventType,
        LocationEventAttributes.LocationDataSource source,
        @Nullable String jobId,
        long attemptTimestamp
    ) {
        if (connectLocation.locationEventListener == null) {
            return;
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put(LocationEventAttributes.LOCATION_EVENT_EVENT_TYPE, eventType.name());
        data.put(LocationEventAttributes.LOCATION_EVENT_JOB_ID, jobId);
        data.put(LocationEventAttributes.LOCATION_EVENT_DELAY_TO_COMPLETE,
            String.valueOf(System.currentTimeMillis() - attemptTimestamp)
        );
        data.put(LocationEventAttributes.LOCATION_EVENT_SOURCE, source.name());
        connectLocation.locationEventListener.onLocationEventReported(LocationEventType.EventUploadSuccessful, data);
    }

    static void logEventUploadFailed(
        ConnectLocation connectLocation,
        @Nullable LocationEventUploader.EventType eventType,
        @Nullable LocationEventAttributes.LocationDataSource source,
        @Nullable String jobId,
        @Nullable LocationEventAttributes.ErrorType errorType,
        @Nullable String errorMessage
    ) {
        if (connectLocation.locationEventListener == null) {
            return;
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put(LocationEventAttributes.LOCATION_EVENT_EVENT_TYPE, eventType.name());
        data.put(LocationEventAttributes.LOCATION_EVENT_JOB_ID, jobId);
        data.put(LocationEventAttributes.LOCATION_EVENT_ERROR_TYPE, errorType.name());
        data.put(LocationEventAttributes.LOCATION_EVENT_ERROR_MESSAGE, errorMessage);
        data.put(LocationEventAttributes.LOCATION_EVENT_SOURCE, source.name());
        connectLocation.locationEventListener.onLocationEventReported(LocationEventType.EventUploadFailed, data);
    }

    private LocationEventHelper() {
        throw new AssertionError();
    }
}
