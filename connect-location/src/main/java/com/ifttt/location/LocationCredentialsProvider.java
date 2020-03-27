package com.ifttt.location;

import androidx.annotation.WorkerThread;
import com.ifttt.connect.Connection;

/**
 * Interface that defines APIs for providing credentials used during the service authentication process for a
 * {@link Connection}.
 */
public interface LocationCredentialsProvider {

    /**
     * @return Your users' IFTTT user token, once they have successfully authenticate your service on IFTTT.
     */
    @WorkerThread
    String getUserToken();
}
