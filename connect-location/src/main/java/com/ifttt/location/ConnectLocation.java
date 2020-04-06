package com.ifttt.location;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import com.ifttt.connect.Connection;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.CredentialsProvider;
import com.ifttt.connect.ErrorResponse;
import com.ifttt.connect.ui.ButtonStateChangeListener;
import com.ifttt.connect.ui.ConnectButton;
import com.ifttt.connect.ui.ConnectButtonState;

public final class ConnectLocation implements ButtonStateChangeListener {

    private static ConnectLocation INSTANCE = null;
    private GeofenceProvider geofenceProvider;
    private ConnectionApiClient connectionApiClient;

    public static synchronized ConnectLocation init(Context context, ConnectionApiClient apiClient) {
        if (INSTANCE == null) {
            INSTANCE = new ConnectLocation(new AwarenessGeofenceProvider(context), apiClient);
        }
        return INSTANCE;
    }

    public static synchronized ConnectLocation init(Context context, CredentialsProvider credentialsProvider) {
        if (INSTANCE == null) {
            ConnectionApiClient.Builder clientBuilder = new ConnectionApiClient.Builder(context);
            INSTANCE = new ConnectLocation(new AwarenessGeofenceProvider(context), clientBuilder.build());
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
    ConnectLocation(GeofenceProvider geofenceProvider, ConnectionApiClient connectionApiClient) {
        this.connectionApiClient = connectionApiClient;
        this.geofenceProvider = geofenceProvider;
    }

    @Override
    public void onStateChanged(
        ConnectButtonState currentState,
        ConnectButtonState previousState,
        Connection connection
    ) {
        if (currentState == ConnectButtonState.Enabled
            || currentState == ConnectButtonState.Disabled
            || currentState == ConnectButtonState.Initial) {
            geofenceProvider.updateGeofences(connection.features);
        }
    }

    @Override
    public void onError(ErrorResponse errorResponse) {
        // No-op
    }
}
