package com.ifttt.groceryexpress

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.LinearLayout
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
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    private lateinit var connectButton: ConnectButton
    private lateinit var toolbar: Toolbar
    private lateinit var emailPreferencesHelper: EmailPreferencesHelper
    private lateinit var credentialsProvider: CredentialsProvider
    private lateinit var connectionId: String

    private lateinit var features: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        emailPreferencesHelper = EmailPreferencesHelper(this)
        credentialsProvider = GroceryExpressCredentialsProvider(emailPreferencesHelper);

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        title = null

        connectButton = findViewById(R.id.connect_button)
        features = findViewById(R.id.features)

        if (savedInstanceState?.containsKey(KEY_CONNECTION_ID) == true) {
            connectionId = savedInstanceState.getString(KEY_CONNECTION_ID)!!
            setUpConnectButton()
            if (connectionId == CONNECTION_ID_LOCATION) {
                setUpLocation()
            }
            return
        }

        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.select_connection))
            .setItems(
                resources.getStringArray(R.array.connections)
            ) { _, connection ->
                if (connection == 0) {
                    connectionId = CONNECTION_ID_GOOGLE_CALENDAR
                } else {
                    connectionId = CONNECTION_ID_LOCATION
                    setUpLocation()
                }
                setUpConnectButton()
            }
            .setCancelable(false)
            .show()
    }

    private fun setUpLocation() {
        ConnectLocation.getInstance().setUpWithConnectButton(connectButton)
    }

    private fun setUpConnectButton() {
        val hasEmailSet = !TextUtils.isEmpty(emailPreferencesHelper.getEmail())
        val suggestedEmail = if (!hasEmailSet) {
            EMAIL
        } else {
            emailPreferencesHelper.getEmail()!!
        }

        val fetchCompleteListener = ConnectButton.OnFetchConnectionListener {
            findViewById<TextView>(R.id.connection_title).text = it.name
            it.features.forEach {
                val featureView = FeatureView(this@MainActivity).apply {
                    text = it.description
                    Picasso.get().load(it.iconUrl).into(this)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        val margin = resources.getDimensionPixelSize(R.dimen.feature_margin_horizontal)
                        setMargins(0, 0, 0, margin)
                    }
                }

                features.addView(featureView)
            }
        }

        val configuration = ConnectButton.Configuration.Builder.withConnectionId(
            connectionId,
            suggestedEmail,
            credentialsProvider
            , REDIRECT_URI
        ).setOnFetchCompleteListener(fetchCompleteListener)
            .build()

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::connectionId.isInitialized) {
            outState.putString(KEY_CONNECTION_ID, connectionId)
        }
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

    companion object {
        const val CONNECTION_ID_GOOGLE_CALENDAR = "fWj4fxYg"
        const val CONNECTION_ID_LOCATION = "pWisyzm7"
        const val EMAIL = "user@email.com"
        const val KEY_CONNECTION_ID = "key_connection_id"
    }
}
