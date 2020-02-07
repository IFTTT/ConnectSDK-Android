package com.ifttt.connect.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;
import java.util.List;
import retrofit2.Response;

/*
 * Schedules a one time work request to read from the queue, make an api call to submit events and remove them from queue.
 * */
public final class AnalyticsEventUploader extends Worker {

    private final AnalyticsManager analyticsManager;
    private final AnalyticsApiHelper apiHelper;

    private static final int MAX_RETRY_COUNT = 3;

    public AnalyticsEventUploader(Context context, WorkerParameters params) {
        super(context, params);
        analyticsManager = AnalyticsManager.getInstance(context.getApplicationContext());
        apiHelper = AnalyticsApiHelper.get(AnalyticsPreferences.getAnonymousId(context));
    }

    @Override
    @NonNull
    public Result doWork() {
        try {
            List<AnalyticsEventPayload> list = analyticsManager.performRead();

            if (list != null && !list.isEmpty()) {
                Response<Void> response = apiHelper
                        .submitEvents(new EventsList(list))
                        .execute();

                if (response.isSuccessful()) {
                    analyticsManager.performRemove(list.size());
                    return Result.success();
                }
            } else {
                return Result.success();
            }
        } catch (IOException e) {
        }

        if (getRunAttemptCount() < MAX_RETRY_COUNT) {
            return Result.retry();
        } else {
            return Result.failure();
        }
    }
}
