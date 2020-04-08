package com.ifttt.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.VisibleForTesting;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceClient;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.ifttt.connect.Connection;
import com.ifttt.connect.Feature;
import com.ifttt.connect.LocationFieldValue;
import com.ifttt.connect.UserFeature;
import com.ifttt.connect.UserFeatureField;
import com.ifttt.connect.UserFeatureStep;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link GeofenceProvider} implementation using {@link Awareness} API. This implementation processes a list of
 * {@link UserFeature} from a {@link Connection}, extract all {@link LocationFieldValue} that is currently enabled for
 * the user, and then use them as the geofence setup.
 *
 * The process includes:
 * - registering any new geofences
 * - un-registering any outdated geofences
 */
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

        exitPendingIntent = PendingIntent.getBroadcast(context,
            REQUEST_CODE_EXIT,
            new Intent(context, AwarenessExitReceiver.class),
            PendingIntent.FLAG_UPDATE_CURRENT
        );

        enterPendingIntent = PendingIntent.getBroadcast(context,
            REQUEST_CODE_ENTER,
            new Intent(context, AwarenessEnterReceiver.class),
            PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    @Override
    public void updateGeofences(final Connection connection) {
        fenceClient.queryFences(FenceQueryRequest.all()).addOnSuccessListener(fenceQueryResponse -> {
            FenceUpdateRequest.Builder requestBuilder = new FenceUpdateRequest.Builder();
            diffFences(connection.status,
                connection.features,
                fenceQueryResponse.getFenceStateMap().getFenceKeys(),
                enterPendingIntent,
                exitPendingIntent,
                new DiffCallback() {
                    @Override
                    public void onAddFence(String key, AwarenessFence value, PendingIntent pendingIntent) {
                        requestBuilder.addFence(key, value, pendingIntent);
                    }

                    @Override
                    public void onRemoveFence(String key) {
                        requestBuilder.removeFence(key);
                    }
                }
            );
            fenceClient.updateFences(requestBuilder.build());
        });
    }

    private static String getEnterFenceKey(String id) {
        return id.concat("/enter");
    }

    private static String getExitFenceKey(String id) {
        return id.concat("/exit");
    }

    interface DiffCallback {
        void onAddFence(String key, AwarenessFence value, PendingIntent pendingIntent);

        void onRemoveFence(String key);
    }

    @VisibleForTesting
    static void diffFences(
        Connection.Status state,
        List<Feature> features,
        Set<String> registeredFenceKeys,
        PendingIntent enterPendingIntent,
        PendingIntent exitPendingIntent,
        DiffCallback callback
    ) {
        Set<String> fenceKeysToRemove = new HashSet<>(registeredFenceKeys);

        for (Feature feature : features) {
            if (feature.userFeatures == null || state != Connection.Status.enabled) {
                // Skip registration if the UserFeature is null or the connection is not enabled.
                continue;
            }

            for (UserFeature userFeature : feature.userFeatures) {
                if (!userFeature.enabled) {
                    // Skip processing disabled UserFeatureSteps.
                    continue;
                }

                for (UserFeatureStep step : userFeature.userFeatureSteps) {
                    for (UserFeatureField userFeatureField : step.fields) {
                        if (!locationFieldTypesList.contains(userFeatureField.fieldType)) {
                            continue;
                        }

                        LocationFieldValue region = (LocationFieldValue) userFeatureField.value;
                        switch (userFeatureField.fieldType) {
                            case FIELD_TYPE_LOCATION_ENTER:
                                if (!fenceKeysToRemove.contains(step.id)) {
                                    callback.onAddFence(step.id,
                                        LocationFence.entering(region.lat, region.lng, region.radius),
                                        enterPendingIntent
                                    );
                                } else {
                                    fenceKeysToRemove.remove(step.id);
                                }
                                break;
                            case FIELD_TYPE_LOCATION_EXIT:
                                if (!fenceKeysToRemove.contains(step.id)) {
                                    callback.onAddFence(step.id,
                                        LocationFence.exiting(region.lat, region.lng, region.radius),
                                        exitPendingIntent
                                    );
                                } else {
                                    fenceKeysToRemove.remove(step.id);
                                }
                                break;
                            case FIELD_TYPE_LOCATION_ENTER_EXIT:
                                String enterFenceKey = getEnterFenceKey(step.id);
                                if (!fenceKeysToRemove.contains(enterFenceKey)) {
                                    callback.onAddFence(enterFenceKey,
                                        LocationFence.entering(region.lat, region.lng, region.radius),
                                        enterPendingIntent
                                    );
                                } else {
                                    fenceKeysToRemove.remove(enterFenceKey);
                                }

                                String exitFenceKey = getExitFenceKey(step.id);
                                if (!fenceKeysToRemove.contains(exitFenceKey)) {
                                    callback.onAddFence(exitFenceKey,
                                        LocationFence.exiting(region.lat, region.lng, region.radius),
                                        exitPendingIntent
                                    );
                                } else {
                                    fenceKeysToRemove.remove(exitFenceKey);
                                }
                                break;
                            default:
                                // No-op for other location types.
                        }
                    }
                }
            }
        }

        // Unregister outdated geofences.
        for (String fenceKey : fenceKeysToRemove) {
            callback.onRemoveFence(fenceKey);
        }
    }
}
