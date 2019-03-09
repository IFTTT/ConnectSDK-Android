package com.ifttt.api.demo

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.textfield.TextInputLayout
import com.ifttt.api.demo.ApiHelper.REDIRECT_URI
import com.ifttt.api.demo.ApiHelper.SERVICE_ID
import com.ifttt.ui.ConnectResult
import com.ifttt.ui.SimpleConnectButton

class MainActivity : AppCompatActivity() {

    private lateinit var iftttConnectButton: SimpleConnectButton
    private lateinit var toolbar: Toolbar
    private lateinit var emailPreferencesHelper: EmailPreferencesHelper
    private lateinit var uiPreferencesHelper: UiPreferencesHelper

    private lateinit var config: SimpleConnectButton.Config

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        emailPreferencesHelper = EmailPreferencesHelper(this)
        uiPreferencesHelper = UiPreferencesHelper(this)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        title = null

        iftttConnectButton = findViewById(R.id.connect_button)

        config = object : SimpleConnectButton.Config {
            override fun getUserToken() = ApiHelper.getUserToken(emailPreferencesHelper.getEmail())

            override fun getOAuthCode() = emailPreferencesHelper.getEmail()

            override fun getRedirectUri() = REDIRECT_URI
        }
        iftttConnectButton.setup(CONNECTION_ID, emailPreferencesHelper.getEmail() ?: EMAIL, SERVICE_ID, config)

        toggleValuePropColor()

        if (emailPreferencesHelper.getEmail() == null) {
            promptLogin()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        menu.findItem(R.id.dark_mode).isChecked = uiPreferencesHelper.getDarkMode()

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.set_email) {
            promptLogin()
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
        iftttConnectButton.setConnectResult(ConnectResult.fromIntent(intent))
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
                iftttConnectButton.setup(CONNECTION_ID, newEmail, SERVICE_ID, config)
            }.setNegativeButton(R.string.logout) { _, _ ->
                emailPreferencesHelper.clear()
                iftttConnectButton.setup(CONNECTION_ID, EMAIL, SERVICE_ID, config)
            }
            .show()
    }

    private companion object {
        const val CONNECTION_ID = "fWj4fxYg"
        const val EMAIL = "user@email.com"
    }
}
