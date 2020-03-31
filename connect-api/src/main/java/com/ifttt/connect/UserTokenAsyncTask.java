package com.ifttt.connect;

import androidx.annotation.Nullable;

public final class UserTokenAsyncTask extends android.os.AsyncTask<Void, Void, String> {

    public interface UserTokenCallback {
        void onComplete();
    }

    private final CredentialsProvider callback;
    private final UserTokenCallback userTokenCallback;
    private final ConnectionApiClient apiClient;

    public UserTokenAsyncTask(CredentialsProvider callback, ConnectionApiClient apiClient,
            UserTokenCallback userTokenCallback) {
        this.callback = callback;
        this.userTokenCallback = userTokenCallback;
        this.apiClient = apiClient;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            return callback.getUserToken();
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }

            return null;
        }
    }

    @Override
    protected void onPostExecute(@Nullable String s) {
        if (s != null) {
            apiClient.setUserToken(s);
        }

        userTokenCallback.onComplete();
    }
}
