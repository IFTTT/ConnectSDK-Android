package com.ifttt.api.demo

import android.app.Activity
import android.os.Bundle

/**
 * Activity for handling Applet configuration redirects.
 */
class DeepLinkActivity: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(MainActivity.redirectIntent(this))
        finish()
    }
}