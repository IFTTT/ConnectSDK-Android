package com.ifttt.location;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.ifttt.connect.api.ConnectionApiClient;
import java.io.IOException;
import java.util.Collections;
import retrofit2.Response;

import static com.ifttt.location.LocationEventUploadHelper.getInstallationId;

/**
 * WorkManager {@link Worker} class responsible for uploading geo-fence events.
 */
public final class LocationEventUploader extends Worker {

    private static final int MAX_RETRY = 3;
    private static final String INPUT_DATA_EVENT_TYPE = "input_event_type";
    private static final String INPUT_DATA_STEP_ID = "input_step_id";

    enum EventType {
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
        try {
            String eventTypeString = getInputData().getString(INPUT_DATA_EVENT_TYPE);
            String stepId = getInputData().getString(INPUT_DATA_STEP_ID);
            if (eventTypeString == null || stepId == null) {
                return Result.failure();
            }

            EventType eventType = EventType.valueOf(eventTypeString);
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

            ConnectLocation location = ConnectLocation.getInstance();
            ConnectionApiClient client = location.connectionApiClient;
            Response<Void> uploadResponse
                = new RetrofitLocationApi.Client(client.interceptor()).api.upload(Collections.singletonList(info))
                .execute();
            if (!uploadResponse.isSuccessful()) {
                Logger.error("Geo-fence event upload failed with status code: " + uploadResponse.code());
                if (uploadResponse.code() == 401) {
                    // The token is invalid, unregister all geo-fences and return.
                    location.deactivate(getApplicationContext());
                    return Result.failure();
                }
                return failureResult();
            }
            Logger.log("Geo-fence event upload successful");
            return Result.success();
        } catch (IOException e) {
            Logger.error("Geo-fence event upload failed with an IOException");
            return failureResult();
        }
    }

    private Result failureResult() {
        if (getRunAttemptCount() > MAX_RETRY) {
            return Result.failure();
        } else {
            return Result.retry();
        }
    }

    static void schedule(Context context, EventType eventType, String stepId) {
        Logger.log("Scheduling geo-fence event upload");
        WorkManager workManager = WorkManager.getInstance(context);
        Data input = new Data.Builder().putString(INPUT_DATA_STEP_ID, stepId).putString(INPUT_DATA_EVENT_TYPE,
            eventType.name()
        ).build();
        workManager.enqueue(new OneTimeWorkRequest.Builder(LocationEventUploader.class).setInputData(input).build());
    }
}
