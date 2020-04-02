package com.ifttt.connect.ui;

import com.ifttt.connect.Connection;
import com.ifttt.connect.ErrorResponse;

/**
 * Callback interface for listening to state changes of the {@link BaseConnectButton}.
 */
public interface ButtonStateChangeListener {
    /**
     * Called when there is a state change, either successful or failed, on the IftttConnectButton.
     *
     * @param currentState Current state of the button.
     * @param previousState Previous state of the button.
     * @param connection Updated connection after state change event.
     */
    void onStateChanged(ConnectButtonState currentState, ConnectButtonState previousState, Connection connection);

    /**
     * Called when the button state change encounters an errorResponse.
     *
     * @param errorResponse ErrorResponse messages from the button or underlying API calls, or null for a successful state
     * change.
     */
    void onError(ErrorResponse errorResponse);
}
