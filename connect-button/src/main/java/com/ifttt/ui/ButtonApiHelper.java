package com.ifttt.ui;

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
import com.ifttt.BuildConfig;
import com.ifttt.Connection;
import com.ifttt.ErrorResponse;
import com.ifttt.IftttApiClient;
import com.ifttt.api.PendingResult;
import com.ifttt.api.PendingResult.ResultCallback;
import com.ifttt.ui.IftttConnectButton.ButtonState;
import java.util.List;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import static com.ifttt.ui.IftttConnectButton.ButtonState.CreateAccount;
import static com.ifttt.ui.IftttConnectButton.ButtonState.Login;
import static com.ifttt.ui.IftttConnectButton.ButtonState.ServiceAuthentication;

/**
 * Helper class that handles all API call and non-UI specific tasks for the {@link IftttConnectButton}.
 */
final class ButtonApiHelper {

    private static final String SHOW_CONNECTION_API_URL = "https://ifttt.com/access/api/";
    private static final String PACKAGE_NAME_IFTTT = "com.ifttt.ifttt.debug";

    private final IftttApiClient iftttApiClient;
    private final OAuthCodeProvider oAuthCodeProvider;
    private final Lifecycle lifecycle;
    private final String redirectUri;
    @Nullable private final String inviteCode;

    @Nullable private String oAuthCode;

    // Default to account existed, so that we don't create unnecessary account through the automatic flow. This is used
    // to help simplify the flow by setting an aggressive timeout for account checking requests.
    private boolean accountFound = true;

    // Reference to the ongoing disable connection call.
    @Nullable private PendingResult<Connection> disableConnectionCall;

    ButtonApiHelper(IftttApiClient client, String redirectUri, @Nullable String inviteCode, OAuthCodeProvider provider,
            Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
        this.redirectUri = redirectUri;
        this.inviteCode = inviteCode;
        this.iftttApiClient = client;
        oAuthCodeProvider = provider;
    }

    void disableConnection(Lifecycle lifecycle, String id, ResultCallback<Connection> resultCallback) {
        disableConnectionCall = iftttApiClient.api().disableConnection(id);
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

    void cancelDisconnect() {
        if (disableConnectionCall == null) {
            return;
        }

        disableConnectionCall.cancel();
        disableConnectionCall = null;
    }

    @CheckReturnValue
    boolean shouldPresentEmail(Context context) {
        Intent launchAppIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(SHOW_CONNECTION_API_URL));
        launchAppIntent.setPackage(PACKAGE_NAME_IFTTT);

        if (hasActivityToLaunch(context, launchAppIntent)) {
            // If the new IFTTT app is installed, always try to redirect there instead of prompting email field.
            return false;
        }

        return !iftttApiClient.isUserAuthenticated();
    }

    void connect(Context context, Connection connection, String email, ButtonState buttonState) {
        Intent launchAppIntent = getIntentToApp(context, connection, email, buttonState);
        if (launchAppIntent != null) {
            context.startActivity(launchAppIntent);
        } else {
            redirectToWeb(context, connection, email, buttonState);
        }
    }

    @SuppressLint("HardwareIds")
    @CheckReturnValue
    private void redirectToWeb(Context context, Connection connection, String email, ButtonState buttonState) {
        EmailAppsChecker checker = new EmailAppsChecker(context.getPackageManager());
        String anonymousId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        Uri uri = getEmbedUri(connection, buttonState, redirectUri, checker.detectEmailApps(), email, anonymousId,
                oAuthCode, inviteCode);
        CustomTabsIntent intent = new CustomTabsIntent.Builder().build();
        intent.launchUrl(context, uri);
    }

    @SuppressLint("HardwareIds")
    @CheckReturnValue
    @Nullable
    private Intent getIntentToApp(Context context, Connection connection, String email, ButtonState buttonState) {
        EmailAppsChecker checker = new EmailAppsChecker(context.getPackageManager());
        String anonymousId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        Intent launchIntent = new Intent(Intent.ACTION_VIEW,
                getEmbedUri(connection, buttonState, redirectUri, checker.detectEmailApps(), email, anonymousId,
                        oAuthCode, inviteCode));
        launchIntent.setPackage(PACKAGE_NAME_IFTTT);

        if (!hasActivityToLaunch(context, launchIntent)) {
            return null;
        }

        return launchIntent;
    }

    boolean shouldPresentCreateAccount(Context context) {
        return shouldPresentEmail(context) && !accountFound;
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
            List<String> emailApps, String email, String anonymousId, @Nullable String oAuthCode,
            @Nullable String inviteCode) {
        Uri.Builder builder = Uri.parse(SHOW_CONNECTION_API_URL + connection.id)
                .buildUpon()
                .appendQueryParameter("sdk_version", BuildConfig.VERSION_NAME)
                .appendQueryParameter("sdk_platform", "android")
                .appendQueryParameter("sdk_return_to", redirectUri)
                .appendQueryParameter("sdk_anonymous_id", anonymousId)
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

        // Append detected email apps to facilitate returning user flow.
        if (buttonState == Login && !emailApps.isEmpty()) {
            for (String emailApp : emailApps) {
                builder.appendQueryParameter("available_email_app_schemes[]", emailApp);
            }
        }

        return builder.build();
    }

    @CheckReturnValue
    static Intent redirectToPlayStore(Context context) {
        Intent launchIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.ifttt.ifttt"));
        launchIntent.setPackage("com.android.vending");

        if (!hasActivityToLaunch(context, launchIntent)) {
            return null;
        }

        return launchIntent;
    }

    @CheckReturnValue
    @Nullable
    static Intent redirectToManage(Context context, String id) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://ifttt.com/connections/" + id));
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