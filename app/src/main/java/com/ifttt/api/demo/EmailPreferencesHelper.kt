package com.ifttt.api.demo

import android.content.Context

/**
 * Helper class for storing email string in SharedPreferences. This simulates the login functionality.
 */
class EmailPreferencesHelper(context: Context, private val defaultEmail: String) {

    private val sharedPreferences = context.getSharedPreferences(preferencesName, 0)

    fun getEmail(): String = sharedPreferences.getString(emailKey, defaultEmail)!!

    fun setEmail(email: String) {
        sharedPreferences.edit().putString(emailKey, email).apply()
    }

    private companion object {
        private const val preferencesName = "demo"
        private const val emailKey = "email"
    }
}
