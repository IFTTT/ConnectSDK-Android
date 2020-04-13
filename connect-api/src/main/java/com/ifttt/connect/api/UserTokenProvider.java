package com.ifttt.connect.api;

import androidx.annotation.WorkerThread;

public interface UserTokenProvider {

    /**
     * @return Your users' IFTTT user token, once they have successfully authenticate your service on IFTTT. Because this
     * method is potentially going to be called for every API call, we recommend caching this value if possible to avoid
     * unnecessary operations.
     */
    @WorkerThread
    String getUserToken();
}
