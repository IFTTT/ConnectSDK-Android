package com.ifttt;

/**
 * Standardized error response from IFTTT API.
 */
public final class ErrorResponse {

    /**
     * A machine-readable string categorizing the failure.
     */
    public final String code;

    /**
     * A human-readable error message describing the failure.
     */
    public final String message;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
