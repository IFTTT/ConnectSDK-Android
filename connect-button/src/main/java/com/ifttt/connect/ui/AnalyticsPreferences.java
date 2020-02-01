package com.ifttt.connect.ui;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

final class AnalyticsPreferences {

    private static AnalyticsPreferences INSTANCE = null;

    private static SharedPreferences preferences;
    private static final String ANALYTICS_OPT_OUT_KEY = "ifttt_analytics_opt_out";
    private static final String ANALYTICS_ANONYMOUS_ID_KEY = "anonymous_id";


    private AnalyticsPreferences(Context context) {
        // Sets shared preference for storing analytics-specific preferences.
        preferences = context.getSharedPreferences("analytics-android", 0);
    }

    static AnalyticsPreferences getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AnalyticsPreferences(context);
        }

        return INSTANCE;
    }

    Boolean getAnalyticsTrackingOptOutPreference() {
        // Default analytics tracking opt-out preference is false
        return preferences.getBoolean(ANALYTICS_OPT_OUT_KEY, false);
    }

    void setAnalyticsTrackingOptOutPreference(Boolean optOut) {
        // Disable analytics tracking.
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(ANALYTICS_OPT_OUT_KEY, optOut);
        editor.apply();
    }

    String getAnonymousId() {
        if (!preferences.contains(ANALYTICS_ANONYMOUS_ID_KEY)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(ANALYTICS_ANONYMOUS_ID_KEY, UUID.randomUUID().toString());
            editor.apply();
        }

        return preferences.getString(ANALYTICS_ANONYMOUS_ID_KEY, null);
    }
 }

