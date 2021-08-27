package com.ifttt.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.awareness.fence.FenceState;

import static com.ifttt.location.LocationEventAttributes.LocationDataSource.Awareness;
import static com.ifttt.location.LocationEventUploader.EventType.Entry;

/**
 * BroadcastReceiver that listens to all "enter" geo-fence events.
 */
public final class AwarenessEnterReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        FenceState fenceState = FenceState.extract(intent);
        if (fenceState.getCurrentState() != FenceState.TRUE) {
            Logger.error("Geo-fence enter event, fence state: " + fenceState.getCurrentState());
            return;
        }

        BackupGeofenceMonitor monitor = BackupGeofenceMonitor.get(context);
        if (BackupGeofenceMonitor.MonitoredGeofence.GeofenceState.Entered
            == monitor.getState(fenceState.getFenceKey())) {
            return;
        }

        Logger.log("Geo-fence enter event");
        LocationEventHelper.logEventReported(ConnectLocation.getInstance(), Entry, Awareness);

        String stepId = LocationEventUploadHelper.extractStepId(fenceState.getFenceKey());
        LocationEventUploader.schedule(context, Entry, Awareness, stepId);
        monitor.setState(fenceState.getFenceKey(), BackupGeofenceMonitor.MonitoredGeofence.GeofenceState.Entered);
    }
}
