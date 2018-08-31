package com.ifttt.api.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.ifttt.IftttApiClient
import com.ifttt.api.PendingResult
import com.ifttt.api.demo.api.ApiHelper
import com.ifttt.api.demo.api.ApiHelper.fetchIftttToken
import com.ifttt.Applet
import com.ifttt.ErrorResponse

class MainActivity : AppCompatActivity() {

    private lateinit var signInRoot: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var userIdEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var startButton: Button
    private lateinit var iftttApiClient: IftttApiClient

    private var appletsPendingResult: PendingResult<List<Applet>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        // Get an instance of the API client.
        iftttApiClient = IftttApiClient.getInstance()

        // For preview services, you'll need to set an invite code for the service in order to use the APIs before
        // the API client has the user token.
        iftttApiClient.setInviteCode(ApiHelper.INVITE_CODE)

        recyclerView = findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        signInRoot = findViewById(R.id.sign_in_root)
        userIdEdit = findViewById(R.id.user_id)
        emailEdit = findViewById(R.id.email)
        startButton = findViewById(R.id.start)

        recyclerView.visibility = View.GONE
        startButton.setOnClickListener { view ->
            val userId = userIdEdit.text.toString()
            if (userId.isEmpty()) {
                userIdEdit.error = getString(R.string.empty_user_id)
                return@setOnClickListener
            }

            val snackBar = Snackbar.make(findViewById<View>(android.R.id.content), R.string.user_auth_error,
                    Snackbar.LENGTH_LONG)
            ApiHelper.login(userId, {
                ApiHelper.fetchIftttToken({ token ->
                    // If the IFTTT token is null, meaning that the given user doesn't have an IFTTT account or
                    // the IFTTT account doesn't have the service connected.
                    if (token != null) {
                        iftttApiClient.setUserToken(token)
                    }
                    renderUi()
                }, {
                    snackBar.show()
                })
            }, {
                snackBar.show()
            })

            // Hide keyboard.
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun renderUi() {
        recyclerView.visibility = View.VISIBLE
        signInRoot.visibility = View.GONE
        startButton.isClickable = false

        appletsPendingResult = iftttApiClient.appletsApi().listApplets(ApiHelper.SERVICE_ID, null, null)
        appletsPendingResult!!.execute(object : PendingResult.ResultCallback<List<Applet>> {
            override fun onSuccess(result: List<Applet>) {
                recyclerView.adapter = AppletAdapter(ArrayList(result),
                        AppletConfigurationClickListenerFactory(userIdEdit.text.toString(),
                                if (emailEdit.text.isEmpty()) null else emailEdit.text.toString()))
            }

            override fun onFailure(errorResponse: ErrorResponse) {
                Snackbar.make(findViewById(android.R.id.content), errorResponse.message, Snackbar.LENGTH_LONG).show()
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.action == ACTION_REDIRECT) {
            // Redirect from Applet configuration flow.
            // After redirecting from the web view, it is possible that the user has connected to your service on IFTTT.
            // In case at this point your app doesn't have IFTTT token, you want to fetch the token again before
            // refreshing the UI. This is to make sure the IFTTT token is attached to IftttApiClient and the Applets
            // retrieved have user status.
            fetchIftttToken({ token ->
                if (token != null) {
                    iftttApiClient.setUserToken(token)
                }
                renderUi()
            }, {
                Snackbar.make(findViewById<View>(android.R.id.content), R.string.user_auth_error,
                        Snackbar.LENGTH_LONG).show()
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        ApiHelper.clearPendingRequests()
        appletsPendingResult?.cancel()
    }

    companion object {
        private const val ACTION_REDIRECT = "action_redirect"

        fun redirectIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).setAction(ACTION_REDIRECT).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }
}
