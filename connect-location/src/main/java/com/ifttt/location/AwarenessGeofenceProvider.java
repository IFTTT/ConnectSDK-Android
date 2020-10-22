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
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.Feature;
import com.ifttt.connect.api.LocationFieldValue;
import com.ifttt.connect.api.UserFeature;
import com.ifttt.connect.api.UserFeatureField;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ifttt.location.LocationEventUploadHelper.extractLocationUserFeatures;
import static com.ifttt.location.LocationEventUploadHelper.getEnterFenceKey;
import static com.ifttt.location.LocationEventUploadHelper.getExitFenceKey;

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
                        Logger.logEvent(this.getClass().getSimpleName(), "Adding geo-fence " + key);
                        requestBuilder.addFence(key, value, pendingIntent);
                    }

                    @Override
                    public void onRemoveFence(String key) {
                        Logger.logEvent(this.getClass().getSimpleName(), "Removing geo-fence " + key);
                        requestBuilder.removeFence(key);
                    }
                }
            );
            fenceClient.updateFences(requestBuilder.build());
        });
    }

    @Override
    public void removeGeofences() {
        fenceClient.updateFences(new FenceUpdateRequest.Builder()
            .removeFence(exitPendingIntent)
            .removeFence(enterPendingIntent)
            .build());
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

        if (state != Connection.Status.enabled) {
            // Unregister outdated geofences.
            for (String fenceKey : fenceKeysToRemove) {
                callback.onRemoveFence(fenceKey);
            }
            return;
        }

        Map<String, List<UserFeatureField<LocationFieldValue>>> locations = extractLocationUserFeatures(features);
        for (Map.Entry<String, List<UserFeatureField<LocationFieldValue>>> entry : locations.entrySet()) {
            String id = entry.getKey();
            for (UserFeatureField<LocationFieldValue> userFeatureField : entry.getValue()) {
                LocationFieldValue region = (LocationFieldValue) userFeatureField.value;
                switch (userFeatureField.fieldType) {
                    case FIELD_TYPE_LOCATION_ENTER:
                        if (!fenceKeysToRemove.contains(id)) {
                            callback.onAddFence(id,
                                LocationFence.entering(region.lat, region.lng, region.radius),
                                enterPendingIntent
                            );
                        } else {
                            fenceKeysToRemove.remove(id);
                        }
                        break;
                    case FIELD_TYPE_LOCATION_EXIT:
                        if (!fenceKeysToRemove.contains(id)) {
                            callback.onAddFence(id,
                                LocationFence.exiting(region.lat, region.lng, region.radius),
                                exitPendingIntent
                            );
                        } else {
                            fenceKeysToRemove.remove(id);
                        }
                        break;
                    case FIELD_TYPE_LOCATION_ENTER_EXIT:
                        String enterFenceKey = getEnterFenceKey(id);
                        if (!fenceKeysToRemove.contains(enterFenceKey)) {
                            callback.onAddFence(enterFenceKey,
                                LocationFence.entering(region.lat, region.lng, region.radius),
                                enterPendingIntent
                            );
                        } else {
                            fenceKeysToRemove.remove(enterFenceKey);
                        }

                        String exitFenceKey = getExitFenceKey(id);
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

        // Unregister outdated geofences.
        for (String fenceKey : fenceKeysToRemove) {
            callback.onRemoveFence(fenceKey);
        }
    }
}
