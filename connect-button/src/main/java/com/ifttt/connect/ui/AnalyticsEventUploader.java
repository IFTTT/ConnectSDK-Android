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

    private AnalyticsManager analyticsManager;
    private static final int MAX_RETRY_COUNT = 3;
    private static AnalyticsApiHelper apiHelper;

    public AnalyticsEventUploader(Context context, WorkerParameters params) {
        super(context, params);
        analyticsManager = AnalyticsManager.getInstance(context.getApplicationContext());
        apiHelper = AnalyticsApiHelper.get(context);
    }

    @Override
    @NonNull
    public Result doWork() {
        Result result;
        try {
            List<AnalyticsEventPayload> list = analyticsManager.performRead();

            if (list != null && !list.isEmpty()) {
                Response<Void> response = apiHelper
                        .submitEvents(new EventsList(list))
                        .execute();

                if (response.isSuccessful()) {
                    analyticsManager.performRemove(list.size());
                    return Result.success();
                } else {
                    result = Result.failure();
                }
            } else {
                return Result.success();
            }
        } catch (IOException e) {
            result = Result.failure();
        }

        if (getRunAttemptCount() < MAX_RETRY_COUNT) {
            return Result.retry();
        } else {
            return result;
        }
    }
}
