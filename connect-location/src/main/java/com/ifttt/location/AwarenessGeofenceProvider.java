package com.ifttt.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceClient;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.ifttt.connect.Connection;
import com.ifttt.connect.Feature;
import com.ifttt.connect.LocationFieldValue;
import com.ifttt.connect.UserFeature;
import com.ifttt.connect.UserFeatureField;
import com.ifttt.connect.UserFeatureStep;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

final class AwarenessGeofenceProvider implements GeofenceProvider {

    private final static int REQUEST_CODE_ENTER = 1001;
    private final static int REQUEST_CODE_EXIT = 1002;

    private final FenceClient fenceClient;
    private final PendingIntent enterPendingIntent;
    private final PendingIntent exitPendingIntent;

    private final static String FIELD_TYPE_LOCATION_ENTER = "LOCATION_ENTER";
    private final static String FIELD_TYPE_LOCATION_EXIT = "LOCATION_EXIT";
    private final static String FIELD_TYPE_LOCATION_ENTER_EXIT = "LOCATION_ENTER_OR_EXIT";

    private final static List<String> locationFieldTypesList = Arrays.asList(FIELD_TYPE_LOCATION_ENTER,
        FIELD_TYPE_LOCATION_EXIT,
        FIELD_TYPE_LOCATION_ENTER_EXIT
    );

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
    public void updateGeofences(final Connection connection) {
        final List<String> updatedIds = new ArrayList<>();

        List<UserFeatureField> userFeatureFields = new ArrayList<>();

        for (Feature feature : connection.features) {
            if (feature.userFeatures == null) {
                continue;
            }

            for (UserFeature userFeature : feature.userFeatures) {
                for (UserFeatureStep step : userFeature.userFeatureSteps) {
                    for (UserFeatureField userFeatureField : step.fields) {
                        if (!locationFieldTypesList.contains(userFeatureField.fieldType)) {
                            continue;
                        }

                        userFeatureFields.add(userFeatureField);
                        if (userFeatureField.fieldType.equals(FIELD_TYPE_LOCATION_ENTER_EXIT)) {
                            updatedIds.add(getEnterFenceKey(userFeatureField.fieldId));
                            updatedIds.add(getExitFenceKey(userFeatureField.fieldId));
                        } else {
                            updatedIds.add(userFeatureField.fieldId);
                        }
                    }
                }
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
                    case FIELD_TYPE_LOCATION_ENTER:
                        requestBuilder.addFence(
                                userFeatureField.fieldId,
                                LocationFence.entering(region.lat, region.lng, region.radius),
                                enterPendingIntent
                        );
                        break;

                    case FIELD_TYPE_LOCATION_EXIT:
                        requestBuilder.addFence(
                                userFeatureField.fieldId,
                                LocationFence.exiting(region.lat, region.lng, region.radius),
                                exitPendingIntent
                        );
                        break;

                    case FIELD_TYPE_LOCATION_ENTER_EXIT:
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
}
