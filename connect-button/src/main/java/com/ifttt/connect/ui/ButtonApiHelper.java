package com.ifttt.connect.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import com.ifttt.connect.BuildConfig;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.ConnectionApiClient;
import com.ifttt.connect.api.ErrorResponse;
import com.ifttt.connect.api.PendingResult;
import com.ifttt.connect.api.PendingResult.ResultCallback;
import java.util.List;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import static com.ifttt.connect.ui.ConnectButtonState.CreateAccount;
import static com.ifttt.connect.ui.ConnectButtonState.Login;

/**
 * Helper class that handles all API call and non-UI specific tasks for the {@link BaseConnectButton}.
 */
final class ButtonApiHelper {

    private static final String SHOW_CONNECTION_API_URL = "https://ifttt.com/access/api/";
    private static final Uri IFTTT_URI = Uri.parse("https://ifttt.com");
    private static final String PACKAGE_NAME_IFTTT = "com.ifttt.ifttt";

    private final ConnectionApiClient connectionApiClient;
    private final CredentialsProvider credentialsProvider;
    private final Lifecycle lifecycle;
    private final Uri redirectUri;
    @Nullable private final String inviteCode;

    @Nullable private String oAuthCode;
    @Nullable private String userLogin;

    // Default to account existed, so that we don't create unnecessary account through the automatic flow. This is used
    // to help simplify the flow by setting an aggressive timeout for account checking requests.
    private boolean accountFound = true;

    // Reference to the ongoing disable connection call.
    @Nullable private PendingResult<Connection> disableConnectionCall;
    @Nullable private PendingResult<Connection> reenableConnectionCall;

    ButtonApiHelper(
        ConnectionApiClient client,
        Uri redirectUri,
        @Nullable String inviteCode,
        CredentialsProvider provider,
        Lifecycle lifecycle
    ) {
        this.lifecycle = lifecycle;
        this.redirectUri = redirectUri;
        this.inviteCode = inviteCode;
        this.connectionApiClient = client;
        credentialsProvider = provider;
    }

    void disableConnection(Lifecycle lifecycle, String id, ResultCallback<Connection> resultCallback) {
        disableConnectionCall = connectionApiClient.api().disableConnection(id);
        lifecycle.addObserver(new PendingResultLifecycleObserver<>(disableConnectionCall));
        disableConnectionCall.execute(new ResultCallback<Connection>() {
            @Override
            public void onSuccess(Connection result) {
                disableConnectionCall = null;
                resultCallback.onSuccess(result);
            }

            @Override
            public void onFailure(ErrorResponse errorResponse) {
                disableConnectionCall = null;
                resultCallback.onFailure(errorResponse);
            }
        });

        lifecycle.addObserver(new PendingResultLifecycleObserver<>(disableConnectionCall));
    }

    void reenableConnection(Lifecycle lifecycle, String id, ResultCallback<Connection> resultCallback) {
        reenableConnectionCall = connectionApiClient.api().reenableConnection(id);
        lifecycle.addObserver(new PendingResultLifecycleObserver<>(reenableConnectionCall));
        reenableConnectionCall.execute(new ResultCallback<Connection>() {
            @Override
            public void onSuccess(Connection result) {
                reenableConnectionCall = null;
                resultCallback.onSuccess(result);
            }

            @Override
            public void onFailure(ErrorResponse errorResponse) {
                reenableConnectionCall = null;
                resultCallback.onFailure(errorResponse);
            }
        });
    }

    void cancelDisconnect() {
        if (disableConnectionCall != null) {
            disableConnectionCall.cancel();
            disableConnectionCall = null;
        }

        if (reenableConnectionCall != null) {
            reenableConnectionCall.cancel();
            reenableConnectionCall = null;
        }
    }

    @CheckReturnValue
    boolean shouldPresentEmail(Context context) {
        Intent launchAppIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(SHOW_CONNECTION_API_URL));
        launchAppIntent.setPackage(PACKAGE_NAME_IFTTT);

        if (hasActivityToLaunch(context, launchAppIntent)) {
            // If the new IFTTT app is installed, always try to redirect there instead of prompting email field.
            return false;
        }

        return !connectionApiClient.isUserAuthorized();
    }

    @CheckReturnValue
    boolean shouldPresentCreateAccount(Context context) {
        return shouldPresentEmail(context) && !accountFound;
    }

    @CheckReturnValue
    boolean isUserAuthorized() {
        return connectionApiClient.isUserAuthorized();
    }

    void fetchConnection(Lifecycle lifecycle, String connectionId, ResultCallback<Connection> callback) {
        PendingResult<Connection> pendingResult = connectionApiClient.api().showConnection(connectionId);
        pendingResult.execute(callback);

        lifecycle.addObserver(new PendingResultLifecycleObserver<>(pendingResult));
    }

    void connect(Context context, Connection connection, String email, ConnectButtonState buttonState) {
        Intent launchAppIntent = getIntentToApp(context, connection, email, buttonState);
        if (launchAppIntent != null) {
            context.startActivity(launchAppIntent);
        } else {
            redirectToWeb(context, connection, email, buttonState);
        }
    }

    @SuppressLint("HardwareIds")
    @CheckReturnValue
    private void redirectToWeb(Context context, Connection connection, String email, ConnectButtonState buttonState) {
        EmailAppsChecker checker = new EmailAppsChecker(context.getPackageManager());
        String anonymousId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        Uri uri = getEmbedUri(connection,
            buttonState,
            redirectUri,
            checker.detectEmailApps(),
            email,
            userLogin,
            anonymousId,
            oAuthCode,
            inviteCode
        );
        CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
        intent.launchUrl(context, uri);
    }

    @SuppressLint("HardwareIds")
    @CheckReturnValue
    @Nullable
    private Intent getIntentToApp(
        Context context, Connection connection, String email, ConnectButtonState buttonState
    ) {
        EmailAppsChecker checker = new EmailAppsChecker(context.getPackageManager());
        String anonymousId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        Intent launchIntent = new Intent(Intent.ACTION_VIEW, getEmbedUri(connection,
            buttonState,
            redirectUri,
            checker.detectEmailApps(),
            email,
            userLogin,
            anonymousId,
            oAuthCode,
            inviteCode
        ));
        launchIntent.setPackage(PACKAGE_NAME_IFTTT);

        if (!hasActivityToLaunch(context, launchIntent)) {
            return null;
        }

        return launchIntent;
    }

    @MainThread
    void prepareAuthentication(String email) {
        RedirectPrepAsyncTask task = new RedirectPrepAsyncTask(credentialsProvider,
            connectionApiClient.isUserAuthorized() ? connectionApiClient.api().user() : null,
            email,
            prepResult -> {
                this.oAuthCode = prepResult.oauthToken;
                this.accountFound = prepResult.accountFound;
                this.userLogin = prepResult.userLogin;
            }
        );
        lifecycle.addObserver(new OAuthTokenExchangeTaskObserver(task));
        task.execute();
    }

    /**
     * Generate a URL for configuring this Connection on web view. The URL can include an optional user email, and an
     * option invite code for the service.
     */
    @VisibleForTesting
    static Uri getEmbedUri(
        Connection connection,
        ConnectButtonState buttonState,
        Uri redirectUri,
        List<String> emailApps,
        String email,
        @Nullable String userLogin,
        String anonymousId,
        @Nullable String oAuthCode,
        @Nullable String inviteCode
    ) {
        Uri.Builder builder = Uri.parse(SHOW_CONNECTION_API_URL + connection.id)
            .buildUpon()
            .appendQueryParameter(
                "sdk_version",
                BuildConfig.VERSION_NAME
            )
            .appendQueryParameter("sdk_platform", "android")
            .appendQueryParameter("sdk_return_to", redirectUri.toString())
            .appendQueryParameter("sdk_anonymous_id", anonymousId);

        if (inviteCode != null) {
            builder.appendQueryParameter("invite_code", inviteCode);
        }

        if (buttonState == CreateAccount) {
            builder.appendQueryParameter("sdk_create_account", "true");
        }

        if ((buttonState == CreateAccount || buttonState == Login) && oAuthCode != null) {
            // Only append the opaque token if we are creating a new account or logging into an existing account.
            builder.appendQueryParameter("code", oAuthCode);
        }

        // Append detected email apps to facilitate returning user flow.
        if (buttonState == Login && !emailApps.isEmpty()) {
            for (String emailApp : emailApps) {
                builder.appendQueryParameter("available_email_app_schemes[]", emailApp);
            }
        }

        if (userLogin != null) {
            builder.appendQueryParameter("username", userLogin);
        } else {
            builder.appendQueryParameter("email", email);
        }

        return builder.build();
    }

    @CheckReturnValue
    static Intent redirectToPlayStore(Context context) {
        Intent launchIntent = new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=com.ifttt.ifttt")
        );
        launchIntent.setPackage("com.android.vending");

        if (!hasActivityToLaunch(context, launchIntent)) {
            return null;
        }

        return launchIntent;
    }

    @CheckReturnValue
    @Nullable
    static Intent redirectToManage(Context context, String id) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
            IFTTT_URI.buildUpon().appendPath("connections").appendPath(id).appendPath("edit").build()
        );
        if (!hasActivityToLaunch(context, intent)) {
            return null;
        }

        return intent;
    }

    @CheckReturnValue
    @Nullable
    static Intent redirectToTerms(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, IFTTT_URI.buildUpon().appendPath("terms").build());
        if (!hasActivityToLaunch(context, intent)) {
            return null;
        }

        return intent;
    }

    @CheckReturnValue
    @Nullable
    static Intent redirectToService(Context context, String moduleName) {
        Intent intent = new Intent(Intent.ACTION_VIEW, IFTTT_URI.buildUpon().appendPath(moduleName).build());
        if (!hasActivityToLaunch(context, intent)) {
            return null;
        }

        return intent;
    }

    @CheckReturnValue
    @Nullable
    static Intent redirectToConnection(Context context, String id) {
        Intent intent = new Intent(
            Intent.ACTION_VIEW,
            IFTTT_URI.buildUpon().appendPath("connections").appendPath(id).build()
        );
        if (!hasActivityToLaunch(context, intent)) {
            return null;
        }

        return intent;
    }

    private static boolean hasActivityToLaunch(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        return !packageManager.queryIntentActivities(intent, 0).isEmpty();
    }

    private static final class OAuthTokenExchangeTaskObserver implements LifecycleObserver {

        private final RedirectPrepAsyncTask asyncTask;

        OAuthTokenExchangeTaskObserver(RedirectPrepAsyncTask asyncTask) {
            this.asyncTask = asyncTask;
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        void onStop() {
            asyncTask.cancel(true);
        }
    }
}
