package com.ifttt.connect.ui;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import retrofit2.Response;

public class AnalyticsEventUploader extends Worker {

    private Context context;

    public AnalyticsEventUploader(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @Override
    @NonNull
    public Result doWork() {
        try {
            int queueSize = AnalyticsManager.getInstance(context).getQueueSize();

            List<Map<String, Object>> list = AnalyticsManager.getInstance(context).performRead(queueSize);
            Log.d("AnalyticsManager", "Inside do work, list= " + list);
            if (list != null) {
                Response<Void> response = AnalyticsApiHelper.get().submitEvents(list).execute();
                if (response.isSuccessful()) {
                    AnalyticsManager.getInstance(context).performRemove(queueSize);
                } else {
                    // TODO: Schedule retries
                }
            }

        } catch(IOException e){
            // TODO: Schedule retries
        }

        return Result.success();
    }
}
