package com.ifttt.groceryexpress

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object UiHelper {

    /**
     * Helper extension function that builds an [Intent] to the app's settings screen.
     */
    fun Context.appSettingsIntent(): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
}
