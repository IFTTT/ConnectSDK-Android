package com.ifttt.location;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.ifttt.connect.Connection;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.CredentialsProvider;
import com.ifttt.connect.ErrorResponse;
import com.ifttt.connect.ui.ButtonStateChangeListener;
import com.ifttt.connect.ui.ConnectButton;
import com.ifttt.connect.ui.ConnectButtonState;
import java.util.concurrent.TimeUnit;

public final class ConnectLocation implements ButtonStateChangeListener {

    private static ConnectLocation INSTANCE = null;
    final GeofenceProvider geofenceProvider;
    final ConnectionApiClient connectionApiClient;
    final CredentialsProvider credentialsProvider;
    private final WorkManager workManager;

    private final String connectionId;

    private final String WORK_ID_CONNECTION_POLLING = "connection_refresh_polling";
    private final long CONNECTION_REFRESH_POLLING_INTERVAL = 1;

    public static synchronized ConnectLocation init(Context context, String connectionId,
            CredentialsProvider credentialsProvider, ConnectionApiClient apiClient) {
        if (INSTANCE == null) {
            INSTANCE = new ConnectLocation(connectionId, new AwarenessGeofenceProvider(context),
                    credentialsProvider, apiClient, WorkManager.getInstance(context));
        }
        return INSTANCE;
    }

    public static synchronized ConnectLocation init(Context context, String connectionId,
            CredentialsProvider credentialsProvider) {
        if (INSTANCE == null) {
            ConnectionApiClient.Builder clientBuilder = new ConnectionApiClient.Builder(context);
            INSTANCE = new ConnectLocation(connectionId, new AwarenessGeofenceProvider(context),
                    credentialsProvider, clientBuilder.build(), WorkManager.getInstance(context));
        }
        return INSTANCE;
    }

    public static ConnectLocation getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("Connect Location is not initialized");
        }
        return INSTANCE;
    }

    public void setUpWithConnectButton(ConnectButton connectButton) {
        connectButton.addButtonStateChangeListener(this);
    }

    @VisibleForTesting
    ConnectLocation(String connectionId, GeofenceProvider geofenceProvider,
            CredentialsProvider credentialsProvider, ConnectionApiClient connectionApiClient,
            WorkManager workManager) {
        this.connectionId = connectionId;
        this.geofenceProvider = geofenceProvider;
        this.credentialsProvider = credentialsProvider;
        this.connectionApiClient = connectionApiClient;
        this.workManager = workManager;
    }

    private void setUpPolling() {
        Constraints constraints =
                new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        workManager.enqueueUniquePeriodicWork(WORK_ID_CONNECTION_POLLING,
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(ConnectionRefresher.class,
                        CONNECTION_REFRESH_POLLING_INTERVAL, TimeUnit.HOURS).setConstraints(
                        constraints).build());
    }

    @Override
    public void onStateChanged(ConnectButtonState currentState, ConnectButtonState previousState,
            Connection connection) {
            if (currentState == ConnectButtonState.Enabled
                    || currentState == ConnectButtonState.Disabled
                    || currentState == ConnectButtonState.Initial) {
                geofenceProvider.updateGeofences(connection);
            }
        if (currentState == ConnectButtonState.Enabled) {
            setUpPolling();
        }
    }

    @Override
    public void onError(ErrorResponse errorResponse) {
        // No-op
    }
}
