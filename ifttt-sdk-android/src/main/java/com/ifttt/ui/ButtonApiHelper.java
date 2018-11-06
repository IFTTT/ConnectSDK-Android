package com.ifttt.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.annotation.MainThread;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import com.ifttt.Applet;
import com.ifttt.BuildConfig;
import com.ifttt.ErrorResponse;
import com.ifttt.IftttApiClient;
import com.ifttt.api.IftttApi;
import com.ifttt.api.PendingResult;
import com.ifttt.api.PendingResult.ResultCallback;
import com.ifttt.ui.IftttConnectButton.ButtonState;
import javax.annotation.Nullable;

import static com.ifttt.ui.IftttConnectButton.ButtonState.CreateAccount;
import static com.ifttt.ui.IftttConnectButton.ButtonState.Login;
import static com.ifttt.ui.IftttConnectButton.ButtonState.ServiceConnection;

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

    ButtonApiHelper(IftttApiClient iftttApiClient, String redirectUri, OAuthCodeProvider provider,
            Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
        this.redirectUri = redirectUri;
        inviteCode = iftttApiClient.getInviteCode();
        iftttApi = iftttApiClient.api();
        oAuthCodeProvider = provider;
    }

    void disableApplet(String appletId, ResultCallback<Applet> resultCallback) {
        PendingResult<Applet> pendingResult = iftttApi.disableApplet(appletId);
        lifecycle.addObserver(new PendingResultLifecycleObserver<>(pendingResult));
        pendingResult.execute(new ResultCallback<Applet>() {
            @Override
            public void onSuccess(Applet result) {
                resultCallback.onSuccess(result);
            }

            @Override
            public void onFailure(ErrorResponse errorResponse) {
                resultCallback.onFailure(errorResponse);
            }
        });
    }

    void redirectToWeb(Context context, Applet applet, String email, ButtonState buttonState) {
        Uri uri = getEmbedUri(applet, buttonState, redirectUri, email, oAuthCode, inviteCode);
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
     * Generate a URL for configuring this Applet on web view. The URL can include an optional user email, and an
     * option invite code for the service.
     */
    private static Uri getEmbedUri(Applet applet, IftttConnectButton.ButtonState buttonState,
            @Nullable String redirectUri, @Nullable String email, @Nullable String oAuthCode,
            @Nullable String inviteCode) {
        Uri.Builder builder = Uri.parse(applet.embeddedUrl)
                .buildUpon()
                .appendQueryParameter("ifttt_sdk_version", BuildConfig.VERSION_NAME)
                .appendQueryParameter("ifttt_sdk_platform", "android");

        if (redirectUri != null) {
            builder.appendQueryParameter("sdk_return_to", redirectUri);
        }

        if (email != null) {
            builder.appendQueryParameter("email", email);
        }

        if (inviteCode != null) {
            builder.appendQueryParameter("invite_code", inviteCode);
        }

        if (buttonState == ServiceConnection) {
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
