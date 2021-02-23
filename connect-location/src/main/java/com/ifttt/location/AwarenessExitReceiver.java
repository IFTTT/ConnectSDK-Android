package com.ifttt.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.awareness.fence.FenceState;

/**
 * BroadcastReceiver that listens to all "exit" geo-fence events.
 */
public final class AwarenessExitReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        FenceState fenceState = FenceState.extract(intent);
        if (fenceState.getCurrentState() != FenceState.TRUE) {
            Logger.error("Geo-fence exit event, fence state: " + fenceState.getCurrentState());
            return;
        }

        BackupGeofenceMonitor monitor = BackupGeofenceMonitor.get(context);
        if (BackupGeofenceMonitor.MonitoredGeofence.GeofenceState.Exited
            == monitor.getState(fenceState.getFenceKey())) {
            return;
        }

        Logger.log("Geo-fence exit event");
        String stepId = LocationEventUploadHelper.extractStepId(fenceState.getFenceKey());
        LocationEventUploader.schedule(context, LocationEventUploader.EventType.Exit, stepId);
        monitor.setState(fenceState.getFenceKey(), BackupGeofenceMonitor.MonitoredGeofence.GeofenceState.Exited);
    }
}
