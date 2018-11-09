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
import com.ifttt.ui.ButtonStateChangeListener
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

    private var connectionPendingResult: PendingResult<Connection>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        connectionContainer = findViewById<ViewGroup>(R.id.connection_view).apply {
            visibility = View.GONE
        }

        icon = findViewById(R.id.icon)
        title = findViewById(R.id.title)
        description = findViewById(R.id.description)
        iftttConnectButton = findViewById(R.id.connect_button)
        iftttConnectButton.setButtonStateChangeListener(object : ButtonStateChangeListener {
            override fun onStateChanged(currentState: IftttConnectButton.ButtonState,
                    previousState: IftttConnectButton.ButtonState) {
                showSnackbar("Current state: ${currentState.name}, previous state: ${previousState.name}")
            }

            override fun onError(errorResponse: ErrorResponse) {
                showSnackbar(errorResponse.message)
            }

        })

        signInRoot = findViewById(R.id.sign_in_root)
        userIdEdit = findViewById(R.id.user_id)
        startButton = findViewById(R.id.start)

        iftttApiClient = IftttApiClient.Builder()
                .setInviteCode(ApiHelper.INVITE_CODE)
                .build()

        startButton.setOnClickListener { view ->
            val userId = userIdEdit.text.toString()
            if (userId.isEmpty()) {
                userIdEdit.error = getString(R.string.empty_user_id)
                return@setOnClickListener
            }

            ApiHelper.login(userId, next = { token ->
                ApiHelper.fetchIftttToken(next = { iftttToken ->
                    if (iftttToken != null) {
                        iftttApiClient.setUserToken(iftttToken)
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
            // The authentication has completed, we can now fetch the user token and refresh the UI.
            ApiHelper.fetchIftttToken(next = {
                if (it != null) {
                    iftttApiClient.setUserToken(it)
                }

                iftttApiClient.api().showConnection(CONNECTION_ID).execute(object :
                        PendingResult.ResultCallback<Connection> {
                    override fun onSuccess(result: Connection) {
                        // After this, users will be able to turn on or off the Connection.
                        iftttConnectButton.setConnection(result)
                    }

                    override fun onFailure(errorResponse: ErrorResponse) {
                        showSnackbar(errorResponse.message)
                    }

                })
            }, error = {
                showSnackbar(getString(R.string.user_auth_error))
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        ApiHelper.clearPendingRequests()
        connectionPendingResult?.cancel()
    }

    private fun renderUi() {
        connectionContainer.visibility = View.VISIBLE
        signInRoot.visibility = View.GONE
        startButton.isClickable = false

        connectionPendingResult = iftttApiClient.api().showConnection(CONNECTION_ID)
        connectionPendingResult!!.execute(object : PendingResult.ResultCallback<Connection> {
            override fun onSuccess(result: Connection) {
                title.text = result.name
                Picasso.get().load(result.primaryService.colorIconUrl).into(icon)
                icon.background = ShapeDrawable(OvalShape()).apply {
                    paint.color = result.primaryService.brandColor
                }
                description.text = result.description
                iftttConnectButton.setup(EMAIL, iftttApiClient, ApiHelper.REDIRECT_URI) {
                    "Your_users_oauth_code"
                }
                iftttConnectButton.setConnection(result)
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
