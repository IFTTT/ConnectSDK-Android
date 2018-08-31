package com.ifttt.api;

import com.ifttt.Applet;

/**
 * API endpoints for configuring Applets, i.e enabling and disabling Applets.
 */
public interface AppletConfigApi {
    /**
     * API for enabling an Applet.
     *
     * @param serviceId Applet owner's service id.
     * @param appletId  Applet id.
     * @return  A {@link PendingResult} for the API call execution.
     */
    PendingResult<Applet> enableApplet(String serviceId, String appletId);

    /**
     * API for disabling an Applet.
     *
     * @param serviceId Applet owner's service id.
     * @param appletId  Applet id.
     * @return A {@link PendingResult} for the API call execution.
     */
    PendingResult<Applet> disableApplet(String serviceId, String appletId);
}
