package com.ifttt.groceryexpress

import android.content.Context

class UiPreferencesHelper(context: Context) {

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, 0)

    fun getDarkMode(): Boolean = sharedPreferences.getBoolean(KEY_DARK_MODE, false)

    fun setDarkMode(dark: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, dark).apply()
    }

    private companion object {
        private const val PREF_NAME = "ui"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
