package com.ifttt.ui;

public enum ConnectButtonState {
    /**
     * A button state for displaying an Connection in its initial state, the user has never authenticated this Connection
     * before.
     */
    Initial,

    /**
     * A button state for the create account authentication step. In this step, the user is going to be redirected
     * to web to create an account and continue with service connection.
     */
    CreateAccount,

    /**
     * A button state for the login authentication step. In this step, the user is going to be redirected to web
     * to login to IFTTT.
     */
    Login,

    /**
     * A button state for service connection step. In this step, the user is going to be redirected to web to
     * login to the service and connect to IFTTT.
     */
    ServiceAuthentication,

    /**
     * A button state for displaying a Connection that is enabled.
     */
    Enabled,

    /**
     * A button stat efor displaying a Connection that is disabled.
     */
    Disabled
}
