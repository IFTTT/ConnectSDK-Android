package com.ifttt.connect.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import java.util.UUID;

/**
 * Helper class to generate and maintain an anonymous Identifier for the installation. Used for analytics purposes.
 */
public final class AnonymousId {

    private static final String ANALYTICS_ANONYMOUS_ID_KEY = "anonymous_id";

    @SuppressLint("HardwareIds")
    public static String get(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId != null) {
            return androidId;
        }

        SharedPreferences preferences = context.getSharedPreferences("ifttt_connect_sdk_anonymous_id", 0);;
        if (!preferences.contains(ANALYTICS_ANONYMOUS_ID_KEY)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(ANALYTICS_ANONYMOUS_ID_KEY, UUID.randomUUID().toString());
            editor.apply();
        }

        return preferences.getString(ANALYTICS_ANONYMOUS_ID_KEY, null);
    }

    private AnonymousId() {
        throw new AssertionError();
    }
 }
