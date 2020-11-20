package com.ifttt.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A {@link BroadcastReceiver} that listens to system's {@link Intent#ACTION_BOOT_COMPLETED} broadcast, and make an
 * attempt to re-register geo-fences.
 */
public final class RebootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            return;
        }

        ConnectionRefresher.executeIfExists(context);
    }
}
