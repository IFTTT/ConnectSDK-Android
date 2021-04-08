package com.ifttt.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;
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
import com.ifttt.location.ConnectLocation.LocationStatusCallback;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ifttt.location.LocationEventUploadHelper.extractLocationUserFeatures;
import static com.ifttt.location.LocationEventUploadHelper.getEnterFenceKey;
import static com.ifttt.location.LocationEventUploadHelper.getExitFenceKey;
import static com.ifttt.location.LocationEventUploadHelper.getIftttFenceKey;
import static com.ifttt.location.LocationEventUploadHelper.isIftttFenceKey;

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
    private final BackupGeofenceMonitor monitor;
    private final PendingIntent enterPendingIntent;
    private final PendingIntent exitPendingIntent;

    AwarenessGeofenceProvider(Context context) {
        this.fenceClient = Awareness.getFenceClient(context);
        this.monitor = BackupGeofenceMonitor.get(context);

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
    public void updateGeofences(final Connection connection, @Nullable LocationStatusCallback locationStatusCallback) {
        monitor.updateMonitoredGeofences(connection.features);

        fenceClient.queryFences(FenceQueryRequest.all()).addOnSuccessListener(fenceQueryResponse -> {

            FenceUpdateRequest.Builder requestBuilder = new FenceUpdateRequest.Builder();
            diffFences(connection.status,
                connection.features,
                fenceQueryResponse.getFenceStateMap().getFenceKeys(),
                enterPendingIntent,
                exitPendingIntent,
                new DiffCallback() {
                    @Override
                    public void onAddFence(String key, AwarenessFence fence, PendingIntent pendingIntent) {
                        Logger.log("Adding geo-fence: " + key);
                        requestBuilder.addFence(key, fence, pendingIntent);
                    }

                    @Override
                    public void onRemoveFence(String key) {
                        Logger.log("Removing geo-fence: " + key);
                        requestBuilder.removeFence(key);
                    }
                }
            );

            fenceClient.updateFences(requestBuilder.build()).continueWithTask(task -> fenceClient.queryFences(
                FenceQueryRequest.all())).addOnSuccessListener(response -> {
                if (locationStatusCallback == null) {
                    return;
                }

                boolean hasActiveGeofence = false;
                for (String key : response.getFenceStateMap().getFenceKeys()) {
                    if (isIftttFenceKey(key)) {
                        hasActiveGeofence = true;
                        break;
                    }
                }
                locationStatusCallback.onLocationStatusUpdated(hasActiveGeofence);

                Logger.log("Geo-fences status updated, activated: " + hasActiveGeofence);
            });
        });
    }

    @Override
    public void removeGeofences(@Nullable LocationStatusCallback locationStatusCallback) {
        fenceClient.updateFences(new FenceUpdateRequest.Builder().removeFence(exitPendingIntent)
            .removeFence(enterPendingIntent)
            .build()).addOnSuccessListener(aVoid -> {
            if (locationStatusCallback != null) {
                locationStatusCallback.onLocationStatusUpdated(false);
            }
        });

        monitor.clear();
    }

    interface DiffCallback {
        void onAddFence(String key, AwarenessFence fence, PendingIntent pendingIntent);

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
        // Because the host app may also use the same FenceClient to manage their own fences, we only monitor and
        // update fence keys set by the SDK.
        Set<String> fenceKeysToRemove = new HashSet<>(registeredFenceKeys);
        for (String key : registeredFenceKeys) {
            if (isIftttFenceKey(key)) {
                fenceKeysToRemove.add(key);
            }
        }

        if (state != Connection.Status.enabled) {
            // Unregister outdated geofences.
            for (String fenceKey : fenceKeysToRemove) {
                callback.onRemoveFence(fenceKey);
            }
            return;
        }

        Map<String, List<UserFeatureField<LocationFieldValue>>> locations = extractLocationUserFeatures(features, true);
        for (Map.Entry<String, List<UserFeatureField<LocationFieldValue>>> entry : locations.entrySet()) {
            String id = getIftttFenceKey(entry.getKey());
            for (UserFeatureField<LocationFieldValue> userFeatureField : entry.getValue()) {
                LocationFieldValue region = userFeatureField.value;
                switch (userFeatureField.fieldType) {
                    case FIELD_TYPE_LOCATION_ENTER:
                        callback.onAddFence(id,
                            LocationFence.entering(region.lat, region.lng, region.radius),
                            enterPendingIntent
                        );
                        fenceKeysToRemove.remove(id);
                        break;
                    case FIELD_TYPE_LOCATION_EXIT:
                        callback.onAddFence(id,
                            LocationFence.exiting(region.lat, region.lng, region.radius),
                            exitPendingIntent
                        );
                        fenceKeysToRemove.remove(id);
                        break;
                    case FIELD_TYPE_LOCATION_ENTER_EXIT:
                        String enterFenceKey = getEnterFenceKey(id);
                        callback.onAddFence(enterFenceKey,
                            LocationFence.entering(region.lat, region.lng, region.radius),
                            enterPendingIntent
                        );
                        fenceKeysToRemove.remove(enterFenceKey);

                        String exitFenceKey = getExitFenceKey(id);
                        callback.onAddFence(exitFenceKey,
                            LocationFence.exiting(region.lat, region.lng, region.radius),
                            exitPendingIntent
                        );
                        fenceKeysToRemove.remove(exitFenceKey);
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
