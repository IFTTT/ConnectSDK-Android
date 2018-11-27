package com.ifttt.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import com.ifttt.BuildConfig;
import com.ifttt.Connection;
import com.ifttt.ErrorResponse;
import com.ifttt.api.IftttApi;
import com.ifttt.api.PendingResult;
import com.ifttt.api.PendingResult.ResultCallback;
import com.ifttt.ui.IftttConnectButton.ButtonState;
import javax.annotation.Nullable;

import static com.ifttt.ui.IftttConnectButton.ButtonState.CreateAccount;
import static com.ifttt.ui.IftttConnectButton.ButtonState.Login;
import static com.ifttt.ui.IftttConnectButton.ButtonState.ServiceAuthentication;

/**
 * Helper class that handles all API call and non-UI specific tasks for the {@link IftttConnectButton}.
 */
final class ButtonApiHelper {

    private final IftttApi iftttApi;
    private final OAuthCodeProvider oAuthCodeProvider;
    private final Lifecycle lifecycle;
    private final String redirectUri;
    private final String inviteCode;

    @Nullable private String oAuthCode;

    // Default to account existed, so that we don't create unnecessary account through the automatic flow. This is used
    // to help simplify the flow by setting an aggressive timeout for account checking requests.
    private boolean accountFound = true;

    ButtonApiHelper(IftttApi iftttApi, String redirectUri, String inviteCode, OAuthCodeProvider provider,
            Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
        this.redirectUri = redirectUri;
        this.inviteCode = inviteCode;
        this.iftttApi = iftttApi;
        oAuthCodeProvider = provider;
    }

    void disableConnection(String id, ResultCallback<Connection> resultCallback) {
        PendingResult<Connection> pendingResult = iftttApi.disableConnection(id);
        lifecycle.addObserver(new PendingResultLifecycleObserver<>(pendingResult));
        pendingResult.execute(new ResultCallback<Connection>() {
            @Override
            public void onSuccess(Connection result) {
                resultCallback.onSuccess(result);
            }

            @Override
            public void onFailure(ErrorResponse errorResponse) {
                resultCallback.onFailure(errorResponse);
            }
        });
    }

    void redirectToWeb(Context context, Connection connection, String email, ButtonState buttonState) {
        Uri uri = getEmbedUri(connection, buttonState, redirectUri, email, oAuthCode, inviteCode);
        CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
        intent.launchUrl(context, uri);
    }

    void redirectToPlayStore(Context context) {
        Intent launchIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.ifttt.ifttt"));
        launchIntent.setPackage("com.android.vending");
        if (!hasActivityToLaunch(context, launchIntent)) {
            // No-op.
            return;
        }

        context.startActivity(launchIntent);
    }

    boolean isAccountFound() {
        return accountFound;
    }

    @MainThread
    void prepareAuthentication(String email) {
        RedirectPrepAsyncTask task = new RedirectPrepAsyncTask(oAuthCodeProvider, email, prepResult -> {
            this.oAuthCode = prepResult.opaqueToken;
            this.accountFound = prepResult.accountFound;
        });
        lifecycle.addObserver(new OAuthTokenExchangeTaskObserver(task));
        task.execute();
    }

    /**
     * Generate a URL for configuring this Connection on web view. The URL can include an optional user email, and an
     * option invite code for the service.
     */
    @VisibleForTesting
    static Uri getEmbedUri(Connection connection, IftttConnectButton.ButtonState buttonState, String redirectUri,
            String email, @Nullable String oAuthCode, @Nullable String inviteCode) {
        Uri.Builder builder = Uri.parse(connection.embeddedUrl)
                .buildUpon()
                .appendQueryParameter("sdk_version", BuildConfig.VERSION_NAME)
                .appendQueryParameter("sdk_platform", "android")
                .appendQueryParameter("sdk_return_to", redirectUri)
                .appendQueryParameter("email", email);

        if (inviteCode != null) {
            builder.appendQueryParameter("invite_code", inviteCode);
        }

        if (buttonState == ServiceAuthentication) {
            builder.appendQueryParameter("skip_sdk_redirect", "true");
        } else if (buttonState == CreateAccount) {
            builder.appendQueryParameter("sdk_create_account", "true");
        }

        if ((buttonState == CreateAccount || buttonState == Login) && oAuthCode != null) {
            // Only append the opaque token if we are creating a new account or logging into an existing account.
            builder.appendQueryParameter("code", oAuthCode);
        }

        return builder.build();
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
