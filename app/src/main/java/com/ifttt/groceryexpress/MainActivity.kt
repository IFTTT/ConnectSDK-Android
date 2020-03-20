package com.ifttt.groceryexpress

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputLayout
import com.ifttt.connect.ConnectionApiClient
import com.ifttt.connect.ui.ConnectButton
import com.ifttt.connect.ui.ConnectResult
import com.ifttt.connect.ui.CredentialsProvider
import com.ifttt.groceryexpress.ApiHelper.REDIRECT_URI

class MainActivity : AppCompatActivity() {

    private lateinit var connectButton: ConnectButton
    private lateinit var toolbar: Toolbar
    private lateinit var emailPreferencesHelper: EmailPreferencesHelper

    private val credentialsProvider = object : CredentialsProvider {
        override fun getUserToken() = ApiHelper.getUserToken(emailPreferencesHelper.getEmail())

        override fun getOAuthCode() = emailPreferencesHelper.getEmail()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        emailPreferencesHelper = EmailPreferencesHelper(this)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        title = null

        connectButton = findViewById(R.id.connect_button)

        val hasEmailSet = !TextUtils.isEmpty(emailPreferencesHelper.getEmail())
        val suggestedEmail = if (!hasEmailSet) {
            EMAIL
        } else {
            emailPreferencesHelper.getEmail()!!
        }

        val configuration = ConnectButton.Configuration.Builder.withConnectionId(
            CONNECTION_ID,
            suggestedEmail,
            credentialsProvider
            , REDIRECT_URI
        ).setOnFetchCompleteListener { connection ->
            findViewById<TextView>(R.id.connection_title).text = connection.name
        }.build()
        connectButton.setup(configuration)

        if (!hasEmailSet) {
            promptLogin()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.set_email) {
            promptLogin()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        connectButton.setConnectResult(ConnectResult.fromIntent(intent))
    }

    // For development and testing purpose: this dialog simulates a login process, where the user enters their
    // email, and the app tries to fetch the IFTTT user token for the user. In the case where the user token
    // is empty, we treat it as the user have never connected the service to IFTTT before.
    private fun promptLogin() {
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
                val configuration = ConnectButton.Configuration.Builder.withConnectionId(
                    CONNECTION_ID,
                    newEmail,
                    credentialsProvider
                    , REDIRECT_URI
                ).setOnFetchCompleteListener { connection ->
                    findViewById<TextView>(R.id.connection_title).text = connection.name
                }.build()
                connectButton.setup(configuration)
            }.setNegativeButton(R.string.logout) { _, _ ->
                emailPreferencesHelper.clear()
                val configuration = ConnectButton.Configuration.Builder.withConnectionId(
                        CONNECTION_ID,
                        EMAIL,
                        credentialsProvider
                        , REDIRECT_URI
                    ).setOnFetchCompleteListener { connection ->
                        findViewById<TextView>(R.id.connection_title).text = connection.name
                    }
                    .setConnectionApiClient(
                        ConnectionApiClient.Builder(this).build()
                    ) // Provide a new ConnectionApiClient to reset the authorization header.
                    .build()
                connectButton.setup(configuration)
            }
            .show()
    }

    private companion object {
        const val CONNECTION_ID = "fWj4fxYg"
        const val EMAIL = "user@email.com"
    }
}
