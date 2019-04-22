package com.ifttt.ui;

import com.ifttt.ErrorResponse;
import com.ifttt.ui.IftttConnectButton.ButtonState;

/**
 * Callback interface for listening to state changes of the {@link IftttConnectButton}.
 */
public interface ButtonStateChangeListener {
    /**
     * Called when there is a state change, either successful or failed, on the IftttConnectButton.
     *
     * @param currentState Current state of the button.
     * @param previousState Previous state of the button.
     */
    void onStateChanged(ButtonState currentState, ButtonState previousState);

    /**
     * Called when the button state change encounters an errorResponse.
     *
     * @param errorResponse ErrorResponse messages from the button or underlying API calls, or null for a successful state
     * change.
     */
    void onError(ErrorResponse errorResponse);
}