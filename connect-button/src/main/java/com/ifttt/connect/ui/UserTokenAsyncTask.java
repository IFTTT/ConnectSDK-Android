package com.ifttt.connect.ui;

import androidx.annotation.Nullable;
import com.ifttt.connect.BuildConfig;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.CredentialsProvider;

class UserTokenAsyncTask extends android.os.AsyncTask<Void, Void, String> {

    public interface UserTokenCallback {
        void onComplete();
    }

    private final CredentialsProvider callback;
    private final UserTokenCallback userTokenCallback;
    private final ConnectionApiClient apiClient;

    UserTokenAsyncTask(CredentialsProvider credentialsProvider, ConnectionApiClient apiClient,
            UserTokenCallback userTokenCallback) {
        this.callback = credentialsProvider;
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
