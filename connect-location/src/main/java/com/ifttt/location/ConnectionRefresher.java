package com.ifttt.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.ConnectionApiClient;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import retrofit2.Response;

import static androidx.core.content.ContextCompat.checkSelfPermission;

public final class ConnectionRefresher extends Worker {

    private static final String INPUT_DATA_CONNECTION_ID = "input_connection_id";
    private static final String WORK_ID_CONNECTION_POLLING = "connection_refresh_polling";
    private static final long CONNECTION_REFRESH_POLLING_INTERVAL = 1;

    public ConnectionRefresher(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        ConnectionApiClient connectionApiClient = ConnectLocation.getInstance().connectionApiClient;

        try {
            String connectionId = getInputData().getString(INPUT_DATA_CONNECTION_ID);

            Response<Connection> connectionResult = connectionApiClient.api()
                .showConnection(Objects.requireNonNull(connectionId))
                .getCall()
                .execute();
            if (connectionResult.isSuccessful()) {
                Connection connection = connectionResult.body();
                if (connection == null) {
                    Logger.error("Connection cannot be null");
                    throw new IllegalStateException("Connection cannot be null");
                }

                Logger.log("Connection fetch successful");
                if (checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    ConnectLocation.getInstance().geofenceProvider.updateGeofences(connection, null);
                }
            }
        } catch (IOException e) {
            Logger.error("Connection fetch failed with an IOException");
            return Result.failure();
        }

        return Result.success();
    }

    public static void schedule(Context context, String connectionId) {
        Logger.log("Schedule connection polling");
        WorkManager workManager = WorkManager.getInstance(context);
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        workManager.enqueueUniquePeriodicWork(WORK_ID_CONNECTION_POLLING,
            ExistingPeriodicWorkPolicy.KEEP,
            new PeriodicWorkRequest.Builder(ConnectionRefresher.class,
                CONNECTION_REFRESH_POLLING_INTERVAL,
                TimeUnit.HOURS
            ).setConstraints(constraints)
                .setInputData(new Data.Builder().putString(INPUT_DATA_CONNECTION_ID, connectionId).build())
                .build()
        );
    }

    public static void cancel(Context context) {
        Logger.log("Cancel connection polling");
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.cancelUniqueWork(WORK_ID_CONNECTION_POLLING);
    }
}
