package com.ifttt.connect.analytics;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;
import retrofit2.Response;

public class AnalyticsEventUploader extends Worker {

    public AnalyticsEventUploader(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        try {
            Response<Void> response = AnalyticsApiHelper.get().submitEvents(getInputData().getString("event_data")).execute();
            if (!response.isSuccessful()) {
                // TODO: Schedule retries
            }
        } catch(IOException e){
            // TODO: Schedule retries
        }

        return Result.success();
    }
}
