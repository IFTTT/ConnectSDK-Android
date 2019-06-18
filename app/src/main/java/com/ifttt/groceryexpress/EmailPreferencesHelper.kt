package com.ifttt.groceryexpress

import android.content.Context

/**
 * Helper class for storing email string in SharedPreferences. This simulates the login functionality.
 */
class EmailPreferencesHelper(context: Context) {

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, 0)

    fun getEmail(): String? = sharedPreferences.getString(EMAIL_KEY, null)

    fun setEmail(email: String) {
        sharedPreferences.edit().putString(EMAIL_KEY, email).apply()
    }

    fun clear() {
        sharedPreferences.edit().remove(EMAIL_KEY).apply()
    }

    private companion object {
        private const val PREF_NAME = "demo"
        private const val EMAIL_KEY = "email"
    }
}
