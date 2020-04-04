package com.ifttt.location;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import androidx.work.WorkManager;
import com.ifttt.connect.Connection;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.CredentialsProvider;
import com.ifttt.connect.ErrorResponse;
import com.ifttt.connect.ui.ButtonStateChangeListener;
import com.ifttt.connect.ui.ConnectButton;
import com.ifttt.connect.ui.ConnectButtonState;
import com.ifttt.connect.UserTokenAsyncTask;

public final class ConnectLocation implements ButtonStateChangeListener {

    private static ConnectLocation INSTANCE = null;
    private final GeofenceProvider geofenceProvider;
    final ConnectionApiClient connectionApiClient;
    final CredentialsProvider credentialsProvider;
    private final WorkManager workManager;

    private final String connectionId;

    public static synchronized ConnectLocation init(Context context, String connectionId, CredentialsProvider credentialsProvider, ConnectionApiClient apiClient) {
        if (INSTANCE == null) {
            INSTANCE = new ConnectLocation(connectionId, new AwarenessGeofenceProvider(context), credentialsProvider, apiClient, WorkManager.getInstance(context));
        }
        return INSTANCE;
    }

    public static synchronized ConnectLocation init(Context context, String connectionId, CredentialsProvider credentialsProvider) {
        if (INSTANCE == null) {
            ConnectionApiClient.Builder clientBuilder = new ConnectionApiClient.Builder(context);
            INSTANCE = new ConnectLocation(connectionId, new AwarenessGeofenceProvider(context), credentialsProvider, clientBuilder.build(), WorkManager.getInstance(context));
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
    ConnectLocation(String connectionId, GeofenceProvider geofenceProvider, CredentialsProvider credentialsProvider, ConnectionApiClient connectionApiClient, WorkManager workManager) {
        this.connectionId = connectionId;
        this.geofenceProvider = geofenceProvider;
        this.credentialsProvider = credentialsProvider;
        this.connectionApiClient = connectionApiClient;
        this.workManager = workManager;

        // Fetch user token, and set up polling
        fetchUserToken();
    }

    private void fetchUserToken() {
        UserTokenAsyncTask asyncTask = new UserTokenAsyncTask(
                credentialsProvider,
                connectionApiClient,
                this::setUpPolling);
        asyncTask.execute();
    }

    private void setUpPolling() {
        /*
        * TODO: Execute periodic work request builder
        * TODO: Check connection state, if disabled stop polling.
        * When the connection is enabled, onStateChanged will be called, and polling will restart there
        * */
    }

    @Override
    public void onStateChanged(ConnectButtonState currentState, ConnectButtonState previousState, Connection connection) {
            if (currentState == ConnectButtonState.Enabled
                    || currentState == ConnectButtonState.Disabled
                    || currentState == ConnectButtonState.Initial) {
                geofenceProvider.updateGeofences(connection.features);
            }


        /*
        * TODO: Check if the user token needs to be refreshed, when the status changes, the user token may have changed.
        * TODO: If user token needs to be refreshed, change this method to accept the `result` and set new user token on the api client.
        * */
        setUpPolling();
    }

    @Override
    public void onError(ErrorResponse errorResponse) {
        // No-op
    }
}
