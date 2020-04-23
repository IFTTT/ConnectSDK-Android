package com.ifttt.groceryexpress

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.ifttt.connect.ui.ConnectButton
import com.ifttt.connect.ui.ConnectResult
import com.ifttt.connect.ui.CredentialsProvider
import com.ifttt.groceryexpress.ApiHelper.REDIRECT_URI
import com.ifttt.location.ConnectLocation
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    private lateinit var emailPreferencesHelper: EmailPreferencesHelper
    private lateinit var credentialsProvider: CredentialsProvider
    private lateinit var connectionId: String

    private lateinit var connectButton: ConnectButton
    private lateinit var toolbar: Toolbar
    private lateinit var features: LinearLayout

    private val fetchCompleteListener = ConnectButton.OnFetchConnectionListener {
        findViewById<TextView>(R.id.connection_title).text = it.name
        it.features.forEach {
            val featureView = FeatureView(this@MainActivity).apply {
                text = it.title
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
            if (connectionId == CONNECTION_ID_LOCATION) {
                setupForLocationConnection()
            } else {
                setupForConnection()
            }
            return
        }

        if (TextUtils.isEmpty(emailPreferencesHelper.getEmail())) {
            promptLogin {
                promptConnectionSelection()
            }
        } else {
            promptConnectionSelection()
        }
    }

    /*
    Demonstrate setting up a connection using Location service with the connect-location module: in addition to calling
    ConnectButton#setup, you should also call ConnectLocation#setUpWithConnectButton to set up the ConnectLocation
    instance to listen to ConnectButton's state changes, which can be used to configure ConnectLocation.
     */
    private fun setupForLocationConnection() {
        setupForConnection()
        ConnectLocation.getInstance().setUpWithConnectButton(connectButton) {
            val permissionGrant = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            if (permissionGrant != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0);
            }
        }
    }

    /*
    Demonstrate setting up a connection.
     */
    private fun setupForConnection() {
        val suggestedEmail = emailPreferencesHelper.getEmail() ?: EMAIL
        val configuration = ConnectButton.Configuration.newBuilder(suggestedEmail, REDIRECT_URI)
            .withConnectionId(connectionId)
            .withCredentialProvider(credentialsProvider)
            .setOnFetchCompleteListener(fetchCompleteListener)
            .build()

        connectButton.setup(configuration)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.set_email) {
            promptLogin {
                if (connectionId == CONNECTION_ID_LOCATION) {
                    setupForLocationConnection()
                } else {
                    setupForConnection()
                }
            }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        /*
         Check if the location permission wasn't granted.
         */
        if (grantResults.firstOrNull { it != PackageManager.PERMISSION_GRANTED } != null) {
            /*
            Possibly show a message or any other error/warning indication for the connection not being able to work as expected
             */
        } else {
            ConnectLocation.getInstance().checkLocationPermission(this, null)
        }
    }

    /*
    For development and testing purpose: this dialog can be used to switch between a regular Connection and a Connection
    with Location service.
     */
    private fun promptConnectionSelection() {
        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.select_connection))
            .setItems(
                resources.getStringArray(R.array.connections)
            ) { _, connection ->
                if (connection == 0) {
                    connectionId = CONNECTION_ID_GOOGLE_CALENDAR
                    setupForConnection()
                } else {
                    connectionId = CONNECTION_ID_LOCATION
                    setupForLocationConnection()
                }
            }
            .setCancelable(false)
            .show()
    }

    /*
    For development and testing purpose: this dialog simulates a login process, where the user enters their
    email, and the app tries to fetch the IFTTT user token for the user. In the case where the user token
    is empty, we treat it as the user have never connected the service to IFTTT before.
    */
    private fun promptLogin(onComplete: () -> Unit) {
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
                onComplete()
            }.setNegativeButton(R.string.logout) { _, _ ->
                emailPreferencesHelper.clear()
                onComplete()
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
