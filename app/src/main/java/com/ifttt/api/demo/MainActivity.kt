package com.ifttt.api.demo

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ifttt.ConnectResult
import com.ifttt.Connection
import com.ifttt.ErrorResponse
import com.ifttt.IftttApiClient
import com.ifttt.api.PendingResult
import com.ifttt.api.demo.api.ApiHelper
import com.ifttt.ui.IftttConnectButton
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    private lateinit var signInRoot: LinearLayout
    private lateinit var connectionContainer: ViewGroup
    private lateinit var userIdEdit: EditText
    private lateinit var startButton: Button
    private lateinit var icon: ImageView
    private lateinit var title: TextView
    private lateinit var description: TextView
    private lateinit var iftttConnectButton: IftttConnectButton

    private lateinit var iftttApiClient: IftttApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        connectionContainer = findViewById<ViewGroup>(R.id.connection_view).apply {
            visibility = View.GONE
        }

        icon = findViewById(R.id.icon)
        title = findViewById(R.id.title)
        description = findViewById(R.id.description)
        signInRoot = findViewById(R.id.sign_in_root)
        userIdEdit = findViewById(R.id.user_id)
        startButton = findViewById(R.id.start)

        iftttApiClient = IftttApiClient.Builder().setInviteCode(ApiHelper.INVITE_CODE).build()
        iftttConnectButton = findViewById<IftttConnectButton>(R.id.connect_button).apply {
            // Setup the Connect Button.
            setup(EMAIL, iftttApiClient, ApiHelper.REDIRECT_URI) {
                // This is a required step to make sure the user doesn't have to connect your service on IFTTT
                // during the Connection enable flow.
                // The code here will be run on a background thread.
                "Your_users_oauth_code"
            }
        }

        startButton.setOnClickListener { view ->
            val userId = userIdEdit.text.toString()
            if (userId.isEmpty()) {
                userIdEdit.error = getString(R.string.empty_user_id)
                return@setOnClickListener
            }

            ApiHelper.login(userId, next = {
                // After login, you may try to fetch the user's IFTTT user token, in the case where the user has already
                // authenticate your service on IFTTT.
                ApiHelper.fetchIftttToken(next = { token ->
                    if (token != null) {
                        iftttApiClient.setUserToken(token)
                    }

                    renderUi()
                }, error = {
                    showSnackbar(getString(R.string.user_auth_error))
                })
            }, error = {
                showSnackbar(getString(R.string.user_auth_error))
            })

            // Hide keyboard.
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onNewIntent(intent: Intent) {
        val authenticationResult = ConnectResult.fromIntent(intent)
        iftttConnectButton.setConnectResult(authenticationResult)

        if (authenticationResult.nextStep == ConnectResult.NextStep.Complete) {
            // The connection is now enabled. Now we need to sync the Connection status from the IFTTT,
            // and refresh the Connect Button.
            // To do so:
            // - fetch IFTTT user token, you can do so by implementing an API from your backend or use
            //   client.api().getUserToken(), providing IFTTT Service key (please do not hard code this in the app)
            //   and user OAuth token.
            // - call client.setUserToken()
            // - call client.api().showConnection() again to get the latest Connection metadata.
            // - call button.setConnection and pass in the metadata
            ApiHelper.fetchIftttToken(next = {
                if (it != null) {
                    iftttApiClient.setUserToken(it)
                }

                renderUi()
            }, error = {
                showSnackbar(getString(R.string.user_auth_error))
            })
        }
    }

    private fun renderUi() {
        connectionContainer.visibility = View.VISIBLE
        signInRoot.visibility = View.GONE
        startButton.isClickable = false

        // Fetch the Connection
        iftttApiClient.api().showConnection(CONNECTION_ID).execute(object : PendingResult.ResultCallback<Connection> {
            override fun onSuccess(result: Connection) {
                iftttConnectButton.setConnection(result)

                title.text = result.name
                Picasso.get().load(result.primaryService.monochromeIconUrl).into(icon)
                icon.background = ShapeDrawable(OvalShape()).apply {
                    paint.color = result.primaryService.brandColor
                }
                description.text = result.description
            }

            override fun onFailure(errorResponse: ErrorResponse) {
                showSnackbar(errorResponse.message)
            }
        })
    }

    private companion object {
        // Example Connection.
        const val CONNECTION_ID = "mZRHhST7"
        const val EMAIL = "user@email.com"
    }
}
