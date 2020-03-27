package com.ifttt.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.RequiresPermission;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceClient;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.ifttt.connect.LocationFieldValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class GeofenceProvider {

    private final static int REQUEST_CODE_ENTER = 1001;
    private final static int REQUEST_CODE_EXIT = 1002;

    private FenceClient fenceClient;
    private Context context;

    GeofenceProvider(Context context) {
        this.context = context;
        this.fenceClient = Awareness.getFenceClient(context);
    }

    void updateGeofences(final List<LocationTriggerField> locationTriggerFields) {
        final List<String> updatedIds = new ArrayList<>();
        for (LocationTriggerField field: locationTriggerFields) {
            if (field.id == null) { throw new IllegalStateException("NativePermission id cannot be null for location."); }
            if (field.permissionName.equals(ConnectLocation.TRIGGER_ID_LOCATION_ENTER_EXIT)) {
                updatedIds.add(getEnterFenceKey(field.id));
                updatedIds.add(getExitFenceKey(field.id));
            } else {
                updatedIds.add(field.id);
            }
        }

        fenceClient.queryFences(
                FenceQueryRequest.all()).addOnSuccessListener(fenceQueryResponse -> {
                    FenceUpdateRequest.Builder requestBuilder = new FenceUpdateRequest.Builder();
                    Set<String> fenceKeys = fenceQueryResponse.getFenceStateMap().getFenceKeys();

                    for (String fenceKey: fenceKeys) {
                        if (!updatedIds.contains(fenceKey)) {
                            requestBuilder.removeFence(fenceKey);
                        }
                    }

                    for (LocationTriggerField locationTriggerField: locationTriggerFields) {
                        LocationFieldValue region = locationTriggerField.locationFieldValue;

                        PendingIntent exitPendingIntent = PendingIntent.getBroadcast(
                                context,
                                REQUEST_CODE_EXIT,
                                new Intent(context, AwarenessExitReceiver.class),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        PendingIntent enterPendingIntent = PendingIntent.getBroadcast(
                                context,
                                REQUEST_CODE_ENTER,
                                new Intent(context, AwarenessEnterReceiver.class),
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        switch (locationTriggerField.permissionName) {
                            case ConnectLocation.TRIGGER_ID_LOCATION_ENTER:
                                requestBuilder.addFence(
                                        locationTriggerField.id,
                                        LocationFence.entering(region.lat, region.lng, region.radius),
                                        enterPendingIntent
                                );
                                break;

                            case ConnectLocation.TRIGGER_ID_LOCATION_EXIT:
                                requestBuilder.addFence(
                                        locationTriggerField.id,
                                        LocationFence.exiting(region.lat, region.lng, region.radius),
                                        exitPendingIntent
                                );
                                break;

                            case ConnectLocation.TRIGGER_ID_LOCATION_ENTER_EXIT:
                                requestBuilder.addFence(
                                        getExitFenceKey(locationTriggerField.id),
                                        LocationFence.exiting(region.lat, region.lng, region.radius),
                                        exitPendingIntent
                                );
                                requestBuilder.addFence(
                                        getEnterFenceKey(locationTriggerField.id),
                                        LocationFence.entering(region.lat, region.lng, region.radius),
                                        enterPendingIntent
                                );
                        }
                        fenceClient.updateFences(requestBuilder.build());
                    }
                });
    }

    private String getEnterFenceKey(String id) {
        return id.concat("/enter");
    }

    private String getExitFenceKey(String id) {
        return id.concat("/exit");
    }

    static String extractLocationTriggerFieldId(String fenceKey) {
        return fenceKey.split("/")[0];
    }
}
