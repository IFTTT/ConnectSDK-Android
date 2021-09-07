package com.ifttt.location;

/**
 * Attributes and meta-data for background location events reported by {@link LocationEventListener}.
 */
public final class LocationEventAttributes {

    /**
     * Location change event types, represented by {@link LocationEventUploader.EventType}.
     */
    public static final String LOCATION_EVENT_EVENT_TYPE = "eventType";

    /**
     * Unique location event upload job ID.
     */
    public static final String LOCATION_EVENT_JOB_ID = "jobId";

    /**
     * Error types encountered during location event upload, represented by {@link ErrorType}.
     */
    public static final String LOCATION_EVENT_ERROR_TYPE = "error";

    /**
     * Free-form text from the errors as additional data.
     */
    public static final String LOCATION_EVENT_ERROR_MESSAGE = "errorMessage";

    /**
     * Timestamp representing the lapsed time between a location event is reported and it is to be uploaded.
     */
    public static final String LOCATION_EVENT_DELAY_TO_UPLOAD = "delayToUpload";

    /**
     * Timestamp representing the time it takes to upload a location event.
     */
    public static final String LOCATION_EVENT_DELAY_TO_COMPLETE = "delayToComplete";

    /**
     * Location change events' source types, represented by {@link LocationDataSource}.
     */
    public static final String LOCATION_EVENT_SOURCE = "source";

    public enum ErrorType {
        Network,
        Sdk
    }

    public enum LocationDataSource {
        LocationReport, Awareness
    }

    private LocationEventAttributes() {
        throw new AssertionError();
    }
}
