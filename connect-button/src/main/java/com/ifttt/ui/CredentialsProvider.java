package com.ifttt.ui;

import androidx.annotation.WorkerThread;
import com.ifttt.Connection;

/**
 * Interface that defines APIs for providing credentials used during the service authentication process for a
 * {@link Connection}.
 */
public interface CredentialsProvider {

    /**
     * @return Your users' OAuth code for your service. This is to be used to automatically authenticate the
     * user to your service on IFTTT during the connection enable flow.
     */
    @WorkerThread
    String getOAuthCode();

    /**
     * @return Your users' IFTTT user token, once they have successfully authenticate your service on IFTTT.
     */
    @WorkerThread
    String getUserToken();
}
