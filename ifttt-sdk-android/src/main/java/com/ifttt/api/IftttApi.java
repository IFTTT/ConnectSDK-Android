package com.ifttt.api;

import com.ifttt.Applet;
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
     * API for showing an Applet from a service.
     *
     * @param appletId Applet id.
     * @return A {@link PendingResult} for the API call execution.
     */
    PendingResult<Applet> showApplet(String appletId);

    /**
     * API for disabling an Applet.
     *
     * @param appletId  Applet id.
     * @return A {@link PendingResult} for the API call execution.
     */
    PendingResult<Applet> disableApplet(String appletId);

    /**
     * API for retrieving information about the IFTTT user, as well as the authentication level of the current
     * IftttApiClient.
     *
     * @return A {@link PendingResult} for the API call execution.
     */
    PendingResult<User> user();

    /**
     * API for exchanging an IFTTT user token. The IFTTT user token may be used to make user authenticated API calls,
     * for example {@link #disableApplet(String)}.
     *
     * @param oAuthToken Your user's OAuth access token or refresh token.
     * @param serviceKey Your IFTTT service key.
     *
     * @return A {@link PendingResult} for the API call execution.
     */
    PendingResult<String> userToken(String oAuthToken, String serviceKey);
}
