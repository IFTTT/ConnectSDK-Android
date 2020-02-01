package com.ifttt.connect.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;
import java.util.List;
import retrofit2.Response;

public class AnalyticsEventUploader extends Worker {

    private static String anonymousId;
    private AnalyticsManager analyticsManager;
    private static final int MAX_RETRY_COUNT = 3;

    public AnalyticsEventUploader(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        analyticsManager = AnalyticsManager.getInstance(context.getApplicationContext());
        anonymousId = AnalyticsPreferences.getInstance(context).getAnonymousId();
    }

    @Override
    @NonNull
    public Result doWork() {
        Result result;
        try {
            int queueSize = analyticsManager.getQueueSize();
            List<AnalyticsEventPayload> list = analyticsManager.performRead(queueSize);

            if (list != null && !list.isEmpty()) {
                Response<Void> response = AnalyticsApiHelper.get()
                        .submitEvents(anonymousId, new EventsList(list))
                        .execute();

                if (response.isSuccessful()) {
                    analyticsManager.performRemove(queueSize);
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

        if (result.equals(Result.failure()) && getRunAttemptCount() < MAX_RETRY_COUNT) {
            return Result.retry();
        } else {
            return result;
        }
    }
}
