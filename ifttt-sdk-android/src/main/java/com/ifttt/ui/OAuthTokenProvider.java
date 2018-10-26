package com.ifttt.ui;

import androidx.annotation.WorkerThread;

public interface OAuthTokenProvider {

    @WorkerThread
    String getOAuthToken();
}
