package com.ifttt.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.ConnectionApiClient;
import java.io.IOException;
import retrofit2.Response;

import static androidx.core.content.ContextCompat.checkSelfPermission;

public final class ConnectionRefresher extends Worker {

    public ConnectionRefresher(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        ConnectionApiClient connectionApiClient = ConnectLocation.getInstance().connectionApiClient;

        try {
            Response<Connection> connectionResult = connectionApiClient.api()
                .showConnection(ConnectLocation.getInstance().connectionId)
                .getCall()
                .execute();
            if (connectionResult.isSuccessful()) {
                Connection connection = connectionResult.body();
                if (connection == null) {
                    throw new IllegalStateException("Connection cannot be null");
                }

                if (checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    ConnectLocation.getInstance().geofenceProvider.updateGeofences(connection);
                }
            }
        } catch (IOException e) {
            return Result.failure();
        }

        return Result.success();
    }
}
