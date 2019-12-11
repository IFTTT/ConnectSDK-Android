package com.ifttt.connect.ui;

import android.os.AsyncTask;
import com.ifttt.connect.BuildConfig;
import com.ifttt.connect.User;
import com.ifttt.connect.api.PendingResult;
import java.io.IOException;
import javax.annotation.Nullable;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * Worker {@link AsyncTask} used for token exchange and account matching.
 */
final class RedirectPrepAsyncTask extends AsyncTask<Void, Void, RedirectPrepAsyncTask.PrepResult> {

    interface OnTokenExchangeListener {
        void onExchanged(PrepResult result);
    }

    private final CredentialsProvider provider;
    private final OnTokenExchangeListener listener;
    private final String email;
    @Nullable private final PendingResult<User> userPendingResult;

    // Null userPendingResult means we don't want to try to fetch the user information.
    RedirectPrepAsyncTask(CredentialsProvider provider, @Nullable PendingResult<User> userPendingResult, String email,
            OnTokenExchangeListener listener) {
        this.provider = provider;
        this.userPendingResult = userPendingResult;
        this.email = email;
        this.listener = listener;
    }

    @Override
    protected PrepResult doInBackground(Void... voids) {
        String username = null;
        boolean accountFound;
        try {
            Response<Void> accountMatchResponse = AccountApiHelper.get().findAccount(email).execute();
            if (userPendingResult != null) {
                Response<User> userResponse = userPendingResult.getCall().execute();
                User user = userResponse.body();
                if (userResponse.isSuccessful() && user != null) {
                    username = user.userLogin;
                }
            }
            accountFound = accountMatchResponse.code() != 404;
        } catch (IOException e) {
            // Intentionally set the flag to true, so that the SDK will know to bring users to the web flow
            // to continue Connection authentication.
            accountFound = true;
        }

        // Attempt to get OAuth code from the CredentialProvider implementation.
        String oAuthCode;
        try {
            oAuthCode = provider.getOAuthCode();
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }

            oAuthCode = null;
        }

        return new PrepResult(oAuthCode, accountFound, username);
    }

    @Override
    protected void onPostExecute(PrepResult token) {
        listener.onExchanged(token);
    }

    static final class PrepResult {
        @Nullable final String oauthToken;
        final boolean accountFound;
        @Nullable final String userLogin;

        PrepResult(@Nullable String oauthToken, boolean accountFound, @Nullable String userLogin) {
            this.oauthToken = oauthToken;
            this.accountFound = accountFound;
            this.userLogin = userLogin;
        }
    }

    /**
     * API helper class that handles APIs that {@link BaseConnectButton} needs for the Connection authentication flow.
     */
    private static final class AccountApiHelper {

        private static AccountApiHelper INSTANCE;

        private final AccountApi accountApi;

        private AccountApiHelper() {
            Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.ifttt.com")
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build();

            accountApi = retrofit.create(AccountApi.class);
        }

        Call<Void> findAccount(String email) {
            return accountApi.findAccount(email);
        }

        static AccountApiHelper get() {
            if (INSTANCE == null) {
                INSTANCE = new AccountApiHelper();
            }

            return INSTANCE;
        }
    }
}
