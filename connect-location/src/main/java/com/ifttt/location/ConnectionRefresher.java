package com.ifttt.location;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.ifttt.connect.BuildConfig;
import com.ifttt.connect.Connection;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.ErrorResponse;
import com.ifttt.connect.api.PendingResult;

public final class ConnectionRefresher extends Worker {

    public ConnectionRefresher(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        String connectionId = getInputData().getString("connectionId");
        if (connectionId == null) {
            throw new IllegalStateException("Connection Id cannot be null");
        }

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

        PendingResult<Connection> pendingResult =
                connectionApiClient.api().showConnection(connectionId);

        pendingResult.execute(new PendingResult.ResultCallback<Connection>() {
            @Override
            public void onSuccess(Connection result) {
                ConnectLocation.getInstance().geofenceProvider.updateGeofences(result);
            }

            @Override
            public void onFailure(ErrorResponse errorResponse) {
                // Do nothing
            }
        });

        return Result.success();
    }
}
