package com.ifttt.ui;

import android.os.AsyncTask;
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

    RedirectPrepAsyncTask(CredentialsProvider provider, String email, OnTokenExchangeListener listener) {
        this.provider = provider;
        this.email = email;
        this.listener = listener;
    }

    @Override
    protected PrepResult doInBackground(Void... voids) {
        try {
            String oAuthCode = provider.getOAuthCode();
            Response<Void> accountMatchResponse = AccountApiHelper.get().findAccount(email).execute();
            boolean accountFound = accountMatchResponse.code() != 404;
            return new PrepResult(oAuthCode, accountFound);
        } catch (IOException e) {
            // Intentionally set the flag to true, so that the SDK will know to bring users to the web flow
            // to continue Connection authentication.
            return new PrepResult(null, true);
        }
    }

    @Override
    protected void onPostExecute(PrepResult token) {
        listener.onExchanged(token);
    }

    static final class PrepResult {
        @Nullable final String opaqueToken;
        final boolean accountFound;

        PrepResult(@Nullable String opaqueToken, boolean accountFound) {
            this.opaqueToken = opaqueToken;
            this.accountFound = accountFound;
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
