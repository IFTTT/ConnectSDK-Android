package com.ifttt.location;

import android.util.Log;

final class Logger {

    private static Boolean canLogEvent = false;

    static void enableLogging() {
        canLogEvent = true;
    }

    static void logEvent(String className, String message) {
        if (canLogEvent)
            Log.d(className, message);
    }
}
