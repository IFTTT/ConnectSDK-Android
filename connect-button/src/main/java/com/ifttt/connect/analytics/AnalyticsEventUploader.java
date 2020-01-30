package com.ifttt.connect.analytics;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;
import java.util.List;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class AnalyticsEventUploader extends Worker {

    private static OkHttpClient.Builder builder = new OkHttpClient.Builder();
    private static OkHttpClient okHttpClient = builder.build();
    private final AnalyticsManager analyticsManager;

    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("connect.ifttt.com")
            .client(okHttpClient)
            .build();

    private EventsApi eventsApi = retrofit.create(EventsApi.class);

    public AnalyticsEventUploader(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        analyticsManager = AnalyticsManager.getInstance(context);
    }


    interface EventsApi {
        @POST("/v2/sdk/events")
        Call<Void> postEvents(@Path("events") List<AnalyticsEventPayload> events);
    }

    @Override
    @NonNull
    public Result doWork() {
        int currentQueueSize = analyticsManager.getCurrentQueueSize();
        // Read all elements of the queue
        List<AnalyticsEventPayload> queueData = analyticsManager.performRead(currentQueueSize);

        try {
            Response<Void> response = eventsApi.postEvents(queueData).execute();
            if (!response.isSuccessful()) {
                // TODO: Schedule retries
            }

            else {
                // Remove the items from payload queue using remove after successfully uploading
                analyticsManager.performRemove(currentQueueSize);
                return Result.success();
            }
        } catch(IOException e){
            // TODO: Schedule retries
        }

        return Result.success();
    }
}
