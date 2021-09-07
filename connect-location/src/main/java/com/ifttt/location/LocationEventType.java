package com.ifttt.location;

/**
 * Representations of the background location events that {@link LocationEventListener} listens to.
 */
public enum LocationEventType {
    /**
     * A location change event (entering or exiting a geofence) is reported to the SDK.
     */
    EventReported,

    /**
     * A location change event is scheduled to be uploaded to IFTTT.
     */
    EventUploadAttempted,

    /**
     * A location change event is uploaded successfully to IFTTT.
     */
    EventUploadSuccessful,

    /**
     * A location change event could not be uploaded to IFTTT.
     */
    EventUploadFailed
}
