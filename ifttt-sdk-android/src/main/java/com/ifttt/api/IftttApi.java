package com.ifttt.api;

import com.ifttt.Applet;
import com.ifttt.User;

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
     * @return A {@link PendingResult} for the API call execution to get the user information.
     */
    PendingResult<User> user();

    PendingResult<String> userToken(String oAuthToken, String serviceKey);
}
