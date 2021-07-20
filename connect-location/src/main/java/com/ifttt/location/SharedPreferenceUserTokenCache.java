package com.ifttt.location;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

final class SharedPreferenceUserTokenCache implements Cache<String> {

    private static final String SHARED_PREF_NAME = "ifttt_user_token_store";
    private static final String PREF_KEY_USER_TOKEN = "ifttt_key_user_token";

    private final SharedPreferences sharedPreferences;

    SharedPreferenceUserTokenCache(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void write(String userToken) {
        sharedPreferences.edit().putString(PREF_KEY_USER_TOKEN, userToken).apply();
    }

    @Override
    @Nullable
    public String read() {
        return sharedPreferences.getString(PREF_KEY_USER_TOKEN, null);
    }

    @Override
    public void clear() {
        sharedPreferences.edit().remove(PREF_KEY_USER_TOKEN).apply();
    }
}
