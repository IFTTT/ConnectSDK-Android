package com.ifttt.api.demo

import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Toast
import com.ifttt.api.demo.api.ApiHelper
import com.ifttt.api.demo.customtabs.CustomTabsHelper
import com.ifttt.api.demo.customtabs.Fallback
import com.ifttt.Applet

/**
 * A factory class that builds [View.OnClickListener] for views. The listener will start a Chrome Custom Tab showing
 * Applet activation/configuration web UI.
 */
class AppletConfigurationClickListenerFactory(private val userId: String, private val email: String?) {

    fun newOnClickListener(applets: List<Applet>, holder: RecyclerView.ViewHolder): View.OnClickListener {
        return View.OnClickListener { v ->
            // For Applets that have never been activated for a user, we need to start the activation flow on the
            // web view.
            val appletAtPosition = applets[holder.adapterPosition]

            // Use the utility method to construct a Uri for Applet activation, including the redirect url that will be
            // used when the activation is completed.
            val configUri = appletAtPosition.getEmbedUri(ApiHelper.REDIRECT_URI, userId, email, ApiHelper.INVITE_CODE)

            // This simulates the flow where you direct users to a web view that automatically log the users in to your
            // service, before starting the Applet activation flow. This helps reduce the friction for users to have to
            // log in to the web view from your app.
            ApiHelper.getLoginUri(configUri, { uri ->
                // Start Applet configuration flow by opening the URL on Chrome Custom Tabs.
                CustomTabsHelper.startAppletConfiguration(v.context, Uri.parse(uri), object : Fallback {
                    override fun onOpenChromeFailed(uri: Uri) {
                        // Provide fallback solution when the Chrome Custom Tabs cannot be opened.
                    }

                })
            }, {
                Toast.makeText(v.context, R.string.login_uri_error, Toast.LENGTH_LONG).show()
            })
        }
    }
}