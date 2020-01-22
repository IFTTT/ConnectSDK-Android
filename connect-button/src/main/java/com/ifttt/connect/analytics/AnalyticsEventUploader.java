package com.ifttt.connect.analytics;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

public class AnalyticsEventUploader extends Worker {

    private static OkHttpClient.Builder builder = new OkHttpClient.Builder();
    private static OkHttpClient okHttpClient = builder.build();

    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://connect.ifttt.com")
            .client(okHttpClient)
            .build();

    public AnalyticsEventUploader(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        // Upload event to IFE endpoint, return failure if there is an error, success otherwise
        return Result.success();
    }
}
