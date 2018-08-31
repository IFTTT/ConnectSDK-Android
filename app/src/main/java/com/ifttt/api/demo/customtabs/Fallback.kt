package com.ifttt.api.demo.customtabs

import android.net.Uri

/**
 * Callback interface for [CustomTabsHelper.startAppletConfiguration] to provide
 * fallback solution when the Chrome Custom Tabs cannot be opened.
 */
interface Fallback {
    /**
     * Called when the Chrome Custom Tabs Intent cannot be resolved on a given device.
     *
     * @param uri URL to be opened in Chrome Custom Tabs.
     */
    fun onOpenChromeFailed(uri: Uri)
}
