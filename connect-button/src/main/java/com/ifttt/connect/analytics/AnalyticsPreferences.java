package com.ifttt.connect.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public class AnalyticsPreferences {

    private static SharedPreferences preferences;
    private static final String ANALYTICS_OPT_OUT_PREFERENCE_KEY = "ifttt_analytics_opt_out";
    private static final String ANALYTICS_ANONYMOUS_ID_KEY = "anonymous_id";

    void create(Context context) {
        // Sets shared preference for storing analytics-specific preferences.
        preferences = context.getSharedPreferences("analytics-android", 0);
    }

    Boolean getAnalyticsTrackingOptOutPreference() {
        // Default analytics tracking opt-out preference is set to false
        return preferences.getBoolean(ANALYTICS_OPT_OUT_PREFERENCE_KEY, false);
    }

    void optOutOfAnalyticsTracking() {
        // Disable analytics tracking.
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(ANALYTICS_OPT_OUT_PREFERENCE_KEY, true);
        editor.apply();
    }

    String getAnonymousId() {
        if (!preferences.contains(ANALYTICS_ANONYMOUS_ID_KEY)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(ANALYTICS_OPT_OUT_PREFERENCE_KEY, UUID.randomUUID().toString());
            editor.apply();
        }

        return preferences.getString(ANALYTICS_ANONYMOUS_ID_KEY, null);
    }
 }
