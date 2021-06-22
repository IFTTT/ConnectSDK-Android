package com.ifttt.location;

import android.util.Log;
import androidx.annotation.Nullable;

final class Logger {

    public interface OnLogCapturedListener {
        void onLogCaptured(String log);
    }

    private static final String TAG = "ConnectLocation";
    private static Boolean canLogEvent = false;
    @Nullable private static OnLogCapturedListener listener;

    static void setLoggingEnabled(Boolean enabled) {
        canLogEvent = enabled;
    }

    static void setListener(@Nullable OnLogCapturedListener listener) {
        Logger.listener = listener;
    }

    static void log(String message) {
        if (listener != null) {
            listener.onLogCaptured(message);
        } else if (canLogEvent) {
            Log.d(TAG, message);
        }
    }
    static void warning(String message) {
        if (listener != null) {
            listener.onLogCaptured(message);
        } else if (canLogEvent) {
            Log.w(TAG, message);
        }
    }
    static void error(String message) {
        if (listener != null) {
            listener.onLogCaptured(message);
        } else if (canLogEvent) {
            Log.e(TAG, message);
        }
    }

    private Logger() {
        throw new AssertionError();
    }
}
