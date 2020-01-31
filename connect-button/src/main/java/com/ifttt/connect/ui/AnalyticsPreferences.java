package com.ifttt.connect.ui;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

final class AnalyticsPreferences {

    private static final String ANALYTICS_ANONYMOUS_ID_KEY = "anonymous_id";

    static String getAnonymousId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("analytics-android", 0);
        if (!preferences.contains(ANALYTICS_ANONYMOUS_ID_KEY)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(ANALYTICS_ANONYMOUS_ID_KEY, UUID.randomUUID().toString());
            editor.apply();
        }

        return preferences.getString(ANALYTICS_ANONYMOUS_ID_KEY, null);
    }
 }
