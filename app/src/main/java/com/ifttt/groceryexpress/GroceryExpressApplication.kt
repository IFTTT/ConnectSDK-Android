package com.ifttt.groceryexpress

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.ifttt.location.ConnectLocation

class GroceryExpressApplication : Application() {

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Initialize Location module here, so that it can set up polling and get the most updated location field values
     */
    override fun onCreate() {
        super.onCreate()
        ConnectLocation.init(this, GroceryExpressCredentialsProvider(EmailPreferencesHelper(this)))
        with(ConnectLocation.getInstance()) {
            setLoggingEnabled(BuildConfig.DEBUG)
            setLocationEventListener { type, data ->
                Log.d(GroceryExpressApplication::class.java.simpleName, "$type $data")
                handler.post {
                    Toast.makeText(this@GroceryExpressApplication, "$type $data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
