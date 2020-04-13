package com.ifttt.connect.ui;

import androidx.annotation.WorkerThread;
import com.ifttt.connect.api.Connection;
import com.ifttt.connect.api.UserTokenProvider;

/**
 * Interface that defines APIs for providing credentials used during the service authentication process for a
 * {@link Connection}.
 */
public interface CredentialsProvider extends UserTokenProvider {

    /**
     * @return Your users' OAuth code for your service. This is to be used to automatically authenticate the
     * user to your service on IFTTT during the connection enable flow.
     */
    @WorkerThread
    String getOAuthCode();
}
