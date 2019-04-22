package com.ifttt.ui;

import androidx.annotation.WorkerThread;

public interface OAuthCodeProvider {

    @WorkerThread
    String getOAuthCode();
}
