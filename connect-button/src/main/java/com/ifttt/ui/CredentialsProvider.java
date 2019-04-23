package com.ifttt.ui;

import androidx.annotation.WorkerThread;

public interface CredentialsProvider {

    @WorkerThread
    String getOAuthCode();

    @WorkerThread
    String getUserToken();
}
