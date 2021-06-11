package com.ifttt.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.common.util.concurrent.ListenableFuture;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.ConnectionApiClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import retrofit2.Response;

import static androidx.core.content.ContextCompat.checkSelfPermission;

public final class ConnectionRefresher extends Worker {

    private static final String INPUT_DATA_CONNECTION_ID = "input_connection_id";
    private static final long CONNECTION_REFRESH_POLLING_INTERVAL = 1;

    @VisibleForTesting static final String WORK_ID_CONNECTION_POLLING = "connection_refresh_polling";

    public ConnectionRefresher(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        if (!ConnectLocation.isInitialized()) {
            ConnectLocation.init(getApplicationContext());
        }

        ConnectLocation connectLocation = ConnectLocation.getInstance();
        ConnectionApiClient connectionApiClient = connectLocation.connectionApiClient;

        try {
            String connectionId = getInputData().getString(INPUT_DATA_CONNECTION_ID);

            Response<Connection> connectionResult = connectionApiClient.api().showConnection(Objects.requireNonNull(
                connectionId)).getCall().execute();
            if (connectionResult.isSuccessful()) {
                Connection connection = connectionResult.body();
                if (connection == null) {
                    throw new IllegalStateException("Connection cannot be null");
                }

                Logger.log("Connection fetch successful: " + connectionId);
                if (checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    connectLocation.geofenceProvider.updateGeofences(connection, null);
                }
            } else {
                Logger.error("Could not fetch connection: "
                    + connectionId
                    + ", status code: "
                    + connectionResult.code());
                if (connectionResult.code() == 401) {
                    // The token is invalid, unregister all geo-fences, clear token cache and return.
                    connectLocation.deactivate(getApplicationContext(), null);
                    new SharedPreferenceUserTokenCache(getApplicationContext()).clear();
                }
                return Result.failure();
            }
        } catch (IOException e) {
            Logger.error("Connection fetch failed with an IOException");
            return Result.failure();
        }

        return Result.success();
    }

    static void schedule(Context context, String connectionId) {
        Logger.log("Schedule connection polling");
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.enqueueUniquePeriodicWork(WORK_ID_CONNECTION_POLLING,
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(ConnectionRefresher.class,
                CONNECTION_REFRESH_POLLING_INTERVAL,
                TimeUnit.HOURS
            ).addTag("connection_id:"
                + connectionId)
                .setConstraints(networkConstraint())
                .setInputData(connectionIdData(connectionId))
                .build()
        );
    }

    static void executeIfExists(Context context) {
        Logger.log("Checking periodic connection refresh job");

        WorkManager workManager = WorkManager.getInstance(context);
        ListenableFuture<List<WorkInfo>> future = workManager.getWorkInfosForUniqueWork(WORK_ID_CONNECTION_POLLING);
        future.addListener(() -> {
            try {
                List<WorkInfo> workInfoList = future.get();
                // If the refresher job is scheduled, we should only have one WorkInfo.
                scheduleOneTimeRefreshWork(workManager, workInfoList);
            } catch (ExecutionException e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
            } catch (InterruptedException e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
            }
        }, Executors.newSingleThreadExecutor());
    }

    static void cancel(Context context) {
        Logger.log("Cancel connection polling");
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.cancelUniqueWork(WORK_ID_CONNECTION_POLLING);
    }

    @VisibleForTesting
    @Nullable
    static UUID scheduleOneTimeRefreshWork(WorkManager workManager, List<WorkInfo> workInfoList) {
        if (workInfoList.size() == 1 && workInfoList.get(0).getState() != WorkInfo.State.CANCELLED) {
            List<String> tags = new ArrayList<>(workInfoList.get(0).getTags());
            for (String tag : tags) {
                Logger.log("Tag: " + tag);
                if (!tag.startsWith("connection_id:")) {
                    continue;
                }

                String connectionId = tag.substring(14);
                Logger.log("Execute connection refresh job: " + connectionId);
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ConnectionRefresher.class).setConstraints(
                    networkConstraint()).setInputData(connectionIdData(connectionId)).build();
                workManager.enqueue(request);

                return request.getId();
            }
        }

        return null;
    }

    private static Constraints networkConstraint() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    private static Data connectionIdData(String connectionId) {
        return new Data.Builder().putString(INPUT_DATA_CONNECTION_ID, connectionId).build();
    }
}
