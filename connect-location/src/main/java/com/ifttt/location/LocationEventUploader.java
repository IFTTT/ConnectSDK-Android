package com.ifttt.location;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.ifttt.connect.api.ConnectionApiClient;
import com.ifttt.location.LocationEventAttributes.LocationDataSource;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import retrofit2.Response;

import static com.ifttt.location.LocationEventUploadHelper.getInstallationId;

/**
 * WorkManager {@link Worker} class responsible for uploading geo-fence events.
 */
public final class LocationEventUploader extends Worker {

    private static final int MAX_RETRY = 3;
    private static final String INPUT_DATA_EVENT_TYPE = "input_event_type";
    private static final String INPUT_DATA_STEP_ID = "input_step_id";
    private static final String INPUT_DATA_JOB_ID = "input_job_id";
    private static final String INPUT_DATA_SCHEDULED_TIMESTAMP = "input_scheduled";
    private static final String INPUT_DATA_LOCATION_DATA_SOURCE = "input_location_data_source";

    /**
     * Enum type representing the type of a geofence event, whether it is entering or exiting.
     */
    public enum EventType {
        Entry, Exit
    }

    public LocationEventUploader(
        @NonNull Context context, @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        ConnectLocation location = ConnectLocation.getInstance();

        String stepId = getInputData().getString(INPUT_DATA_STEP_ID);
        String jobId = getInputData().getString(INPUT_DATA_JOB_ID);
        String eventTypeString = getInputData().getString(INPUT_DATA_EVENT_TYPE);
        String sourceString = getInputData().getString(INPUT_DATA_LOCATION_DATA_SOURCE);
        if (eventTypeString == null || stepId == null || sourceString == null) {
            LocationEventHelper.logEventUploadFailed(location,
                null,
                null,
                jobId,
                LocationEventAttributes.ErrorType.Sdk,
                "null eventType"
            );
            return Result.failure();
        }

        LocationDataSource source = LocationDataSource.valueOf(sourceString);
        EventType eventType = EventType.valueOf(eventTypeString);

        try {
            LocationInfo info;
            switch (eventType) {
                case Entry:
                    info = LocationInfo.entry(stepId, getInstallationId(getApplicationContext()));
                    break;
                case Exit:
                    info = LocationInfo.exit(stepId, getInstallationId(getApplicationContext()));
                    break;
                default:
                    throw new IllegalStateException("Unsupported type: " + eventType);
            }

            long scheduledTimestamp = getInputData().getLong(INPUT_DATA_SCHEDULED_TIMESTAMP, -1);
            LocationEventHelper.logEventUploadAttempted(location, eventType, source, jobId, scheduledTimestamp);

            long attemptTimestamp = System.currentTimeMillis();
            ConnectionApiClient client = location.connectionApiClient;
            Response<Void> uploadResponse
                = new RetrofitLocationApi.Client(client.interceptor()).api.upload(Collections.singletonList(info))
                .execute();
            int code = uploadResponse.code();
            if (!uploadResponse.isSuccessful()) {
                Logger.error("Geo-fence event upload failed with status code: " + code);
                if (code == 401) {
                    LocationEventHelper.logEventUploadFailed(location,
                        eventType,
                        source,
                        jobId,
                        LocationEventAttributes.ErrorType.Network,
                        "401"
                    );

                    // The token is invalid, unregister all geo-fences, clear token cache and return.
                    location.deactivate(getApplicationContext(), null);
                    new SharedPreferenceUserTokenCache(getApplicationContext()).clear();
                    return Result.failure();
                }

                return failureResult(location, eventType, source, jobId, String.valueOf(code));
            }
            Logger.log("Geo-fence event upload successful");
            LocationEventHelper.logEventUploadSuccessful(location, eventType, source, jobId, attemptTimestamp);

            return Result.success();
        } catch (IOException e) {
            Logger.error("Geo-fence event upload failed with an IOException");
            return failureResult(location, eventType, source, jobId, e.getMessage());
        }
    }

    private Result failureResult(
        ConnectLocation connectLocation,
        EventType eventType,
        LocationDataSource source,
        @Nullable String jobId,
        @Nullable String error
    ) {
        if (getRunAttemptCount() > MAX_RETRY) {
            LocationEventHelper.logEventUploadFailed(connectLocation,
                eventType,
                source,
                jobId,
                LocationEventAttributes.ErrorType.Network,
                error
            );
            return Result.failure();
        } else {
            return Result.retry();
        }
    }

    static void schedule(
        Context context, EventType eventType, LocationDataSource source, String stepId
    ) {
        Logger.log("Scheduling geo-fence event upload");

        String jobId = UUID.randomUUID().toString();
        WorkManager workManager = WorkManager.getInstance(context);
        Data input = new Data.Builder().putString(INPUT_DATA_STEP_ID, stepId)
            .putString(INPUT_DATA_EVENT_TYPE,
                eventType.name()
            )
            .putString(INPUT_DATA_JOB_ID, jobId)
            .putLong(INPUT_DATA_SCHEDULED_TIMESTAMP, System.currentTimeMillis())
            .putString(
                INPUT_DATA_LOCATION_DATA_SOURCE,
                source.name()
            )
            .build();
        workManager.enqueue(new OneTimeWorkRequest.Builder(LocationEventUploader.class).setInputData(input).build());
    }
}
