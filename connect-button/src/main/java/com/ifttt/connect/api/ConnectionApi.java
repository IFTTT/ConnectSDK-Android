package com.ifttt.connect.api;

import com.ifttt.connect.Connection;
import com.ifttt.connect.ConnectionApiClient;
import com.ifttt.connect.User;

/**
 * IFTTT API wrapper interface. You may use the instance from {@link ConnectionApiClient#api()} to make API calls
 * asynchronously.
 *
 * @see ConnectionApiClient#api()
 */
public interface ConnectionApi {

    /**
     * API for fetching a Connection's metadata.
     *
     * @param id Connection id.
     * @return A {@link PendingResult} for the API call execution.
     */
    PendingResult<Connection> showConnection(String id);

    /**
     * API for disabling a Connection.
     *
     * @param id  Connection id.
     * @return A {@link PendingResult} for the API call execution.
     */
    PendingResult<Connection> disableConnection(String id);

    /**
     * API for re-enable a Connection
     *
     * @param id  Connection id.
     * @return A {@link PendingResult} for the API call execution.
     */
    PendingResult<Connection> reenableConnection(String id);

    /**
     * API for retrieving information about the IFTTT user, as well as the authentication level of the current
     * ConnectionApiClient.
     *
     * @return A {@link PendingResult} for the API call execution.
     */
    PendingResult<User> user();
}
