package com.ifttt.connect.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import retrofit2.Response;

public class AnalyticsEventUploader extends Worker {

    private static String anonymousId;
    private AnalyticsManager analyticsManager;

    public AnalyticsEventUploader(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        analyticsManager = AnalyticsManager.getInstance(context);
        anonymousId = AnalyticsPreferences.getAnonymousId(context);
    }

    @Override
    @NonNull
    public Result doWork() {
        try {
            int queueSize = analyticsManager.getQueueSize();
            List<Map<String, String>> list = analyticsManager.performRead(queueSize);

            if (list != null && !list.isEmpty()) {
                Response<Void> response = AnalyticsApiHelper.get()
                        .submitEvents(anonymousId, new EventsList(list))
                        .execute();

                if (response.isSuccessful()) {
                    analyticsManager.performRemove(queueSize);
                } else {
                    // TODO: Schedule retries
                }
            }
        } catch (IOException e) {
            // TODO: Schedule retries
        }

        return Result.success();
    }
}
