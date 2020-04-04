package com.ifttt.location;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.ifttt.connect.Connection;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.ErrorResponse;
import com.ifttt.connect.api.PendingResult;

public final class ConnectionRefresher extends Worker {

    private final ConnectionApiClient connectionApiClient;

    public ConnectionRefresher(Context context, WorkerParameters params) {
        super(context, params);
        connectionApiClient = ConnectLocation.getInstance().connectionApiClient;
    }

    @Override
    @NonNull
    public Result doWork() {
        String connectionId = getInputData().getString("connectionId");
        if (connectionId == null) {
            throw new IllegalStateException("Connection Id cannot be null");
        }

        PendingResult<Connection> pendingResult = connectionApiClient.api().showConnection(connectionId);

        pendingResult.execute(new PendingResult.ResultCallback<Connection>() {
            @Override
            public void onSuccess(Connection result) {

            }

            @Override
            public void onFailure(ErrorResponse errorResponse) {

            }
        });
        return Result.success();
    }
}
