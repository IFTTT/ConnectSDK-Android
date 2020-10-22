package com.ifttt.location;

import android.util.Log;

final class Logger {

    private static final String TAG = "ConnectLocation";
    private static Boolean canLogEvent = false;

    static void enableLogging() {
        canLogEvent = true;
    }

    static void disableLogging() {
        canLogEvent = false;
    }

    static void log(String message) {
        if (canLogEvent) {
            Log.d(TAG, message);
        }
    }
    static void warning(String message) {
        if (canLogEvent) {
            Log.w(TAG, message);
        }
    }
    static void error(String message) {
        if (canLogEvent) {
            Log.e(TAG, message);
        }
    }

    private Logger() {
        throw new AssertionError();
    }
}
