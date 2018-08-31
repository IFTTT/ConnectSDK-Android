package com.ifttt.api.demo.customtabs

import android.content.Context
import android.net.Uri
import android.support.customtabs.CustomTabsIntent

object CustomTabsHelper {

    /**
     * Utility method, given a fully constructed URL, launch a Chrome Custom Tab to display Applet configuration flow.
     * A [Fallback] is required to be provided when for any reason, the Chrome Custom Tab Intent cannot be
     * resolved.
     */
    fun startAppletConfiguration(context: Context, uri: Uri, fallback: Fallback) {
        val intent = CustomTabsIntent.Builder().build()
        val packageManager = context.packageManager
        if (packageManager.queryIntentActivities(intent.intent, 0).size == 0) {
            fallback.onOpenChromeFailed(uri)
            return
        }

        intent.launchUrl(context, uri)
    }
}