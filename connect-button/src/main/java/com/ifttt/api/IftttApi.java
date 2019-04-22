package com.ifttt.api;

import com.ifttt.Connection;
import com.ifttt.IftttApiClient;
import com.ifttt.User;

/**
 * IFTTT API wrapper interface. You may use the instance from {@link IftttApiClient#api()} to make API calls
 * asynchronously.
 *
 * @see IftttApiClient#api()
 */
public interface IftttApi {

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
     * API for retrieving information about the IFTTT user, as well as the authentication level of the current
     * IftttApiClient.
     *
     * @return A {@link PendingResult} for the API call execution.
     */
    PendingResult<User> user();

    /**
     * API for exchanging an IFTTT user token. The IFTTT user token may be used to make user authenticated API calls,
     * for example {@link #disableConnection(String)}.
     *
     * @param oAuthToken Your user's OAuth access token.
     * @param serviceKey Your IFTTT service key.
     * @param userId User's id in your service.
     *
     * @return A {@link PendingResult} for the API call execution.
     */
    PendingResult<String> userToken(String oAuthToken, String userId, String serviceKey);
}
