package com.ifttt.connect.analytics;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.List;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

public class AnalyticsEventUploader extends Worker {

    private static OkHttpClient.Builder builder = new OkHttpClient.Builder();
    private static OkHttpClient okHttpClient = builder.build();
    private final AnalyticsManager analyticsManager;

    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://connect.ifttt.com")
            .client(okHttpClient)
            .build();

    public AnalyticsEventUploader(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        analyticsManager = AnalyticsManager.getInstance(context);
    }

    @Override
    @NonNull
    public Result doWork() {
        int currentQueueSize = analyticsManager.getCurrentQueueSize();
        // Read all elements of the queue
        List<AnalyticsEventPayload> queueData = analyticsManager.performRead(currentQueueSize);

        // Serialize the payload and make a POST request via Retrofit

        // Remove the items from payload queue using remove after successfully uploading
        analyticsManager.performRemove(currentQueueSize);

        // Handle failures within this method
        return Result.success();
    }
}
