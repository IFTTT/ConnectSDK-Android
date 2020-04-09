package com.ifttt.location;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.ifttt.connect.BuildConfig;
import com.ifttt.connect.Connection;
import com.ifttt.connect.ConnectionApiClient;
import java.io.IOException;
import retrofit2.Response;

public final class ConnectionRefresher extends Worker {

    public ConnectionRefresher(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        ConnectionApiClient connectionApiClient = ConnectLocation.getInstance().connectionApiClient;

        if (!connectionApiClient.isUserAuthorized()) {
            try {
                String userToken = ConnectLocation.getInstance().credentialsProvider.getUserToken();
                connectionApiClient.setUserToken(userToken);
            } catch (Exception e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
                return Result.failure();
            }
        }

        try {
            Response<Connection> connectionResult = connectionApiClient.api().showConnection(ConnectLocation.getInstance().connectionId).getCall().execute();
            if (connectionResult.isSuccessful()) {
                Connection connection = connectionResult.body();
                if (connection == null) {
                    throw new IllegalStateException("Connection cannot be null");
                }
                ConnectLocation.getInstance().geofenceProvider.updateGeofences(connection);
            }
        } catch (IOException e) {
            return Result.failure();
        }

        return Result.success();
    }
}
