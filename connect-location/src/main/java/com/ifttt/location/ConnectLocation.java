package com.ifttt.location;

import android.content.Context;
import android.util.Log;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.CredentialsProvider;
import com.ifttt.connect.ErrorResponse;
import com.ifttt.connect.Feature;
import com.ifttt.connect.LocationFieldValue;
import com.ifttt.connect.UserFeatureField;
import com.ifttt.connect.ui.ButtonStateChangeListener;
import com.ifttt.connect.ui.ConnectButton;
import com.ifttt.connect.ui.ConnectButtonState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ConnectLocation implements ButtonStateChangeListener {

    private static ConnectLocation INSTANCE = null;
    private AwarenessGeofenceProvider awarenessGeofenceProvider;
    private ConnectionApiClient connectionApiClient;

    final static String FIELD_TYPE_LOCATION_ENTER = "LOCATION_ENTER";
    final static String FIELD_TYPE_LOCATION_EXIT = "LOCATION_EXIT";
    final static String FIELD_TYPE_LOCATION_ENTER_EXIT =
            "LOCATION_ENTER_OR_EXIT";

    final static List<String> locationFieldTypesList = Arrays.asList(FIELD_TYPE_LOCATION_ENTER, FIELD_TYPE_LOCATION_EXIT, FIELD_TYPE_LOCATION_ENTER_EXIT);

    public static synchronized ConnectLocation init(Context context, ConnectionApiClient apiClient) {
        if (INSTANCE == null) {
            INSTANCE = new ConnectLocation(context);
        }
        INSTANCE.connectionApiClient = apiClient;
        return INSTANCE;
    }

    public static synchronized ConnectLocation init(Context context, CredentialsProvider credentialsProvider) {
        if (INSTANCE == null) {
            INSTANCE = new ConnectLocation(context);
        }
        ConnectionApiClient.Builder clientBuilder = new ConnectionApiClient.Builder(context);
        INSTANCE.connectionApiClient = clientBuilder.build();
        return INSTANCE;
    }

    private ConnectLocation(Context context) {
        awarenessGeofenceProvider = new AwarenessGeofenceProvider(context);
    }

    public static void setUpWithConnectButton(ConnectButton connectButton) {
        if (INSTANCE == null) {
            throw new IllegalStateException("Connect Location is not initialized");
        }
        connectButton.addButtonStateChangeListener(INSTANCE);
    }

    @Override
    public void onConnectionEnabled(List<Feature> connectionFeatures) {
        // Filter the location based enabled user feature fields.
        // Using hard-coded data till API is ready

        List<UserFeatureField> userFeatureFields = new ArrayList<>();

        awarenessGeofenceProvider.updateGeofences(userFeatureFields);
    }

    @Override
    public void onConnectionDisabled() {
        // Remove all previously registered geofences.
        awarenessGeofenceProvider.updateGeofences(Collections.emptyList());
    }

    @Override
    public void onStateChanged(ConnectButtonState currentState, ConnectButtonState previousState) {
        // No-op
    }

    @Override
    public void onError(ErrorResponse errorResponse) {
        // No-op
    }

}
