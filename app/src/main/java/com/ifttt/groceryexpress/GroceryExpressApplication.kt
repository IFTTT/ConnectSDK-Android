package com.ifttt.groceryexpress

import android.app.Application
import com.ifttt.groceryexpress.NotificationsHelper.sendNotification
import com.ifttt.location.ConnectLocation

class GroceryExpressApplication : Application() {

    /**
     * Initialize Location module here, so that it can set up polling and get the most updated location field values
     */
    override fun onCreate() {
        super.onCreate()
        ConnectLocation.init(this, GroceryExpressCredentialsProvider(EmailPreferencesHelper(this)))
        with(ConnectLocation.getInstance()) {
            setLoggingEnabled(BuildConfig.DEBUG)
            setLocationEventListener { type, data ->
                sendNotification("$type $data")
            }
        }
    }

}
