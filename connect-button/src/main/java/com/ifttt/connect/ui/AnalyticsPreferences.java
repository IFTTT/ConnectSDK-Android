package com.ifttt.connect.ui;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public final class AnalyticsPreferences {

    private static final String ANALYTICS_DISABLED_KEY = "ifttt_analytics_disabled";
    private static final String ANALYTICS_ANONYMOUS_ID_KEY = "anonymous_id";

    static private SharedPreferences getSharedPreferences(Context context) {
        // Sets shared preference for storing analytics-specific preferences.
        return context.getSharedPreferences("analytics-android", 0);
    }

    static boolean getAnalyticsTrackingOptOutPreference(Context context) {
        // Default analytics tracking opt-out preference is false
        return getSharedPreferences(context).getBoolean(ANALYTICS_DISABLED_KEY, false);
    }

    static void setAnalyticsTrackingOptOutPreference(Context context, boolean analyticsDisabled) {
        // Disable analytics tracking.
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean(ANALYTICS_DISABLED_KEY, analyticsDisabled);
        editor.apply();
    }

    static String getAnonymousId(Context context) {
        SharedPreferences preferences = getSharedPreferences(context);
        if (!preferences.contains(ANALYTICS_ANONYMOUS_ID_KEY)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(ANALYTICS_ANONYMOUS_ID_KEY, UUID.randomUUID().toString());
            editor.apply();
        }

        return preferences.getString(ANALYTICS_ANONYMOUS_ID_KEY, null);
    }
 }

