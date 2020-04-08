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
import com.ifttt.connect.CredentialsProvider
import com.ifttt.groceryexpress.ApiHelper.REDIRECT_URI
import com.ifttt.location.ConnectLocation
import org.w3c.dom.Text

class MainActivity : AppCompatActivity() {

    private lateinit var connectButton: ConnectButton
    private lateinit var toolbar: Toolbar
    private lateinit var emailPreferencesHelper: EmailPreferencesHelper
    private lateinit var connectionId: String

    private lateinit var valueProps1: TextView
    private lateinit var valueProps2: TextView
    private lateinit var valueProps3: TextView

    private val credentialsProvider = object :
        CredentialsProvider {
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
        valueProps1 = findViewById(R.id.value_prop_1)
        valueProps2 = findViewById(R.id.value_prop_2)
        valueProps3 = findViewById(R.id.value_prop_3)

        val setUpConnectButton = {
            val hasEmailSet = !TextUtils.isEmpty(emailPreferencesHelper.getEmail())
            val suggestedEmail = if (!hasEmailSet) {
                EMAIL
            } else {
                emailPreferencesHelper.getEmail()!!
            }

            val configuration = ConnectButton.Configuration.Builder.withConnectionId(
                connectionId,
                suggestedEmail,
                credentialsProvider
                , REDIRECT_URI
            ).setOnFetchCompleteListener { connection ->
                findViewById<TextView>(R.id.connection_title).text = connection.name
            }.build()

            connectButton.setup(configuration)

            ConnectLocation.init(this, connectionId, credentialsProvider)
            ConnectLocation.getInstance().setUpWithConnectButton(connectButton)

            if (!hasEmailSet) {
                promptLogin()
            }
        }

        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.select_connection))
            .setItems(
                resources.getStringArray(R.array.connections)
            ) { _, connection ->
                if (connection == 0) {
                    connectionId = CONNECTION_ID_GOOGLE_CALENDAR
                    valueProps1.text = resources.getString(R.string.value_props_1_gcal_connection)
                    valueProps2.text = resources.getString(R.string.value_props_2_gcal_connection)
                    valueProps3.text = resources.getString(R.string.value_props_3_gcal_connection)
                } else {
                    connectionId = CONNECTION_ID_LOCATION
                    valueProps1.text = resources.getString(R.string.value_props_1_loc_connection)
                    valueProps2.text = resources.getString(R.string.value_props_2_loc_connection)
                    valueProps3.text = resources.getString(R.string.value_props_3_loc_connection)
                }
                setUpConnectButton()
            }
            .setCancelable(false)
            .create()
            .show()
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
                    connectionId,
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
                        connectionId,
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
        const val CONNECTION_ID_GOOGLE_CALENDAR = "fWj4fxYg"
        const val CONNECTION_ID_LOCATION = "pWisyzm7"
        const val EMAIL = "user@email.com"
    }
}
