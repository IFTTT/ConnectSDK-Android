package com.ifttt.api.demo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.ifttt.ConnectResult
import com.ifttt.Connection
import com.ifttt.ErrorResponse
import com.ifttt.IftttApiClient
import com.ifttt.api.PendingResult
import com.ifttt.api.demo.ApiHelper.REDIRECT_URI
import com.ifttt.api.demo.ApiHelper.SERVICE_ID
import com.ifttt.ui.IftttConnectButton
import com.ifttt.ui.OAuthCodeProvider

class MainActivity : AppCompatActivity() {

    private lateinit var iftttConnectButton: IftttConnectButton
    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var emailPreferencesHelper: EmailPreferencesHelper
    private lateinit var uiPreferencesHelper: UiPreferencesHelper

    private lateinit var iftttApiClient: IftttApiClient

    private val apiCallback = object : ApiHelper.Callback {
        override fun onSuccess(token: String?) {
            if (token != null) {
                iftttApiClient.setUserToken(token)
            }

            renderUi()
        }

        override fun onFailure(code: String?) {
            if (code == "unauthorized") {
                renderUi()
            } else {
                showSnackbar(getString(R.string.network_request_error))
            }
        }
    }

    private val oAuthCodeProvider = OAuthCodeProvider {
        // This is a required step to make sure the user doesn't have to connect your service on IFTTT
        // during the Connection enable flow.
        // The code here will be run on a background thread.
        emailPreferencesHelper.getEmail()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        emailPreferencesHelper = EmailPreferencesHelper(this, EMAIL)
        uiPreferencesHelper = UiPreferencesHelper(this)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        title = null

        progressBar = findViewById(R.id.progress_bar)

        iftttApiClient = IftttApiClient.Builder(this).setInviteCode(ApiHelper.INVITE_CODE).build()
        iftttConnectButton = findViewById(R.id.connect_button)

        val email = emailPreferencesHelper.getEmail()
        iftttConnectButton.setup(email, SERVICE_ID, iftttApiClient, REDIRECT_URI, oAuthCodeProvider)

        showLoading()
        renderUi()
        toggleValuePropColor()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        menu.findItem(R.id.dark_mode).isChecked = uiPreferencesHelper.getDarkMode()

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.set_email) {
            // For development and testing purpose: this dialog simulates a login process, where the user enters their
            // email, and the app tries to fetch the IFTTT user token for the user. In the case where the user token
            // is empty, we treat it as the user have never connected the service to IFTTT before.
            val emailView =
                LayoutInflater.from(this).inflate(
                    R.layout.view_email,
                    findViewById(Window.ID_ANDROID_CONTENT),
                    false
                ) as TextInputLayout
            emailView.editText!!.setText(emailPreferencesHelper.getEmail())

            AlertDialog.Builder(this)
                .setView(emailView)
                .setTitle(R.string.email_title)
                .setPositiveButton(R.string.login) { _, _ ->
                    val newEmail = emailView.editText!!.text.toString()
                    emailPreferencesHelper.setEmail(newEmail)
                    iftttConnectButton.setup(newEmail, SERVICE_ID, iftttApiClient, REDIRECT_URI, oAuthCodeProvider)

                    // Refresh user token.
                    showLoading()
                    ApiHelper.getUserToken(newEmail, apiCallback)
                }.setNegativeButton(R.string.logout) { _, _ ->
                    recreate()
                }
                .show()
            return true
        } else if (item.itemId == R.id.dark_mode) {
            val toggled = !item.isChecked
            uiPreferencesHelper.setDarkMode(toggled)

            recreate()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val result = ConnectResult.fromIntent(intent)
        iftttConnectButton.setConnectResult(result)

        if (result.nextStep == ConnectResult.NextStep.Complete) {
            // The connection is now enabled. Now we need to sync the Connection status from the IFTTT,
            // and refresh the Connect Button.
            // To do so:
            // - fetch IFTTT user token, you can do so by implementing an API from your backend or use
            //   client.api().getUserToken(), providing IFTTT Service key (please do not hard code this in the app)
            //   and user OAuth token.
            // - call client.setUserToken()
            // - call client.api().showConnection() again to get the latest Connection metadata.
            // - call button.setConnection and pass in the metadata
            ApiHelper.getUserToken(emailPreferencesHelper.getEmail(), apiCallback)
        }
    }

    private fun renderUi() {
        iftttApiClient.api().showConnection(CONNECTION_ID).execute(object : PendingResult.ResultCallback<Connection> {
            override fun onSuccess(result: Connection) {
                showButton()

                iftttConnectButton.setConnection(result)
            }

            override fun onFailure(errorResponse: ErrorResponse) {
                showSnackbar(errorResponse.message)
            }
        })
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        iftttConnectButton.visibility = View.GONE
    }

    private fun showButton() {
        progressBar.visibility = View.GONE
        iftttConnectButton.visibility = View.VISIBLE
    }

    private fun toggleValuePropColor() {
        // For testing and debugging purpose: toggling this changes the UI of the app as well as the Connect Button.
        val darkMode = uiPreferencesHelper.getDarkMode()

        val textColor: Int
        val backgroundColor: Int
        if (darkMode) {
            textColor = Color.WHITE
            backgroundColor = Color.BLACK
        } else {
            textColor = Color.BLACK
            backgroundColor = Color.WHITE
        }

        // If the Activity that Connect Button is displayed on has a dark background, call this function to toggle
        // its look to adapt the UI.
        iftttConnectButton.setOnDarkBackground(darkMode)

        window.setBackgroundDrawable(ColorDrawable(backgroundColor))
        findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT).post {
            val valueProp1 = findViewById<TextView>(R.id.value_prop_1)
            valueProp1.setTextColor(textColor)
            DrawableCompat.setTint(DrawableCompat.wrap(valueProp1.compoundDrawables[0]), textColor)
            val valueProp2 = findViewById<TextView>(R.id.value_prop_2)
            valueProp2.setTextColor(textColor)
            DrawableCompat.setTint(DrawableCompat.wrap(valueProp2.compoundDrawables[0]), textColor)
            val valueProp3 = findViewById<TextView>(R.id.value_prop_3)
            valueProp3.setTextColor(textColor)
            DrawableCompat.setTint(DrawableCompat.wrap(valueProp3.compoundDrawables[0]), textColor)
        }
    }

    private companion object {
        const val CONNECTION_ID = "fWj4fxYg"
        const val EMAIL = "user@email.com"
    }

    private fun Activity.showSnackbar(charSequence: CharSequence) {
        Snackbar.make(findViewById(Window.ID_ANDROID_CONTENT), charSequence, Snackbar.LENGTH_LONG).show()
    }
}
