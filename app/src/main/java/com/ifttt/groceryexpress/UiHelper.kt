package com.ifttt.groceryexpress

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

object UiHelper {

    /**
     * Helper extension function that builds an [Intent] to the app's settings screen.
     */
    fun Context.appSettingsIntent(): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }

    /**
     * Helper extension function that checks a list of permissions to see if they are all granted.
     *
     * @return true if all permissions within the list are granted, false otherwise.
     */
    fun Array<String>.allPermissionsGranted(context: Context): Boolean =
        all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
}
