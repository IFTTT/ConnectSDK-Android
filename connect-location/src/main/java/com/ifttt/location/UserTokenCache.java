package com.ifttt.location;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

final class UserTokenCache {

    private static final String SHARED_PREF_NAME = "ifttt_user_token_store";
    private static final String PREF_KEY_USER_TOKEN = "ifttt_key_user_token";

    private final SharedPreferences sharedPreferences;

    UserTokenCache(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }

    void store(String userToken) {
        sharedPreferences.edit().putString(PREF_KEY_USER_TOKEN, userToken).apply();
    }

    @Nullable
    String get() {
        return sharedPreferences.getString(PREF_KEY_USER_TOKEN, null);
    }

    void clear() {
        sharedPreferences.edit().remove(PREF_KEY_USER_TOKEN).apply();
    }
}
