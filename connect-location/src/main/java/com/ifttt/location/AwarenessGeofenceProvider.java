package com.ifttt.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceClient;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.ifttt.connect.LocationFieldValue;
import com.ifttt.connect.UserFeatureField;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class AwarenessGeofenceProvider implements GeofenceProvider {

    private final static int REQUEST_CODE_ENTER = 1001;
    private final static int REQUEST_CODE_EXIT = 1002;

    private final FenceClient fenceClient;
    private final PendingIntent enterPendingIntent;
    private final PendingIntent exitPendingIntent;

    AwarenessGeofenceProvider(Context context) {
        this.fenceClient = Awareness.getFenceClient(context);

        exitPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_EXIT,
                new Intent(context, AwarenessExitReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        enterPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ENTER,
                new Intent(context, AwarenessEnterReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    @Override
    public void updateGeofences(final List<UserFeatureField> userFeatureFields) {
        final List<String> updatedIds = new ArrayList<>();
        for (UserFeatureField field: userFeatureFields) {
            if (field.fieldId == null) { throw new IllegalStateException("Field Id cannot be null for location feature fields"); }
            if (field.fieldType.equals(ConnectLocation.FIELD_TYPE_LOCATION_ENTER_EXIT)) {
                updatedIds.add(getEnterFenceKey(field.fieldId));
                updatedIds.add(getExitFenceKey(field.fieldId));
            } else {
                updatedIds.add(field.fieldId);
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

            for (UserFeatureField userFeatureField: userFeatureFields) {
                LocationFieldValue region = (LocationFieldValue) userFeatureField.value;

                switch (userFeatureField.fieldType) {
                    case ConnectLocation.FIELD_TYPE_LOCATION_ENTER:
                        requestBuilder.addFence(
                                userFeatureField.fieldId,
                                LocationFence.entering(region.lat, region.lng, region.radius),
                                enterPendingIntent
                        );
                        break;

                    case ConnectLocation.FIELD_TYPE_LOCATION_EXIT:
                        requestBuilder.addFence(
                                userFeatureField.fieldId,
                                LocationFence.exiting(region.lat, region.lng, region.radius),
                                exitPendingIntent
                        );
                        break;

                    case ConnectLocation.FIELD_TYPE_LOCATION_ENTER_EXIT:
                        requestBuilder.addFence(
                                getExitFenceKey(userFeatureField.fieldId),
                                LocationFence.exiting(region.lat, region.lng, region.radius),
                                exitPendingIntent
                        );
                        requestBuilder.addFence(
                                getEnterFenceKey(userFeatureField.fieldId),
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
