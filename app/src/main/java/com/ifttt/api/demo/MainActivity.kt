package com.ifttt.api.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
    private lateinit var emailPreferencesHelper: EmailPreferencesHelper

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
                showSnackbar(getString(R.string.user_auth_error))
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

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        title = null

        iftttApiClient = IftttApiClient.Builder().setInviteCode(ApiHelper.INVITE_CODE).build()
        iftttConnectButton = findViewById(R.id.connect_button)

        val email = emailPreferencesHelper.getEmail()
        iftttConnectButton.setup(email, SERVICE_ID, iftttApiClient, REDIRECT_URI, oAuthCodeProvider)

        ApiHelper.getUserToken(email, apiCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.set_email) {
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
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val newEmail = emailView.editText!!.text.toString()
                    emailPreferencesHelper.setEmail(newEmail)
                    iftttConnectButton.setup(newEmail, SERVICE_ID, iftttApiClient, REDIRECT_URI, oAuthCodeProvider)

                    // Refresh user token.
                    ApiHelper.getUserToken(newEmail, apiCallback)
                }.show()
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
                iftttConnectButton.setConnection(result)
            }

            override fun onFailure(errorResponse: ErrorResponse) {
                showSnackbar(errorResponse.message)
            }
        })
    }

    private companion object {
        const val CONNECTION_ID = "dngPHVFe"
        const val EMAIL = "user@email.com"
    }

    private fun Activity.showSnackbar(charSequence: CharSequence) {
        Snackbar.make(findViewById(Window.ID_ANDROID_CONTENT), charSequence, Snackbar.LENGTH_LONG).show()
    }
}
