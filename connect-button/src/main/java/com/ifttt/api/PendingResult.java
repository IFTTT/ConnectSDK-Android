package com.ifttt.api;

import com.ifttt.ErrorResponse;
import retrofit2.Call;

/**
 * Wrapper interface for the Retrofit {@link Call}. It is used to provide standardized error response we have in the
 * API through {@link ErrorResponse}.
 */
public interface PendingResult<T> {

    /**
     * Callback interface for the API call results.
     */
    interface ResultCallback<T> {

        /**
         * Called when the API call was successful.
         *
         * @param result API response object.
         */
        void onSuccess(T result);

        /**
         * Called when the API call was failed.
         *
         * @param errorResponse Formatted error responses from the API call.
         */
        void onFailure(ErrorResponse errorResponse);
    }

    /**
     * @return the wrapped Retrofit Call object. This can be used to access the additional functionality that Call
     * have, e.g. synchronous execution.
     */
    Call<T> getCall();

    /**
     * Execute the API call, and subscribe to its response.
     *
     * @param callback Callback that provides API call response status and response.
     */
    void execute(ResultCallback<T> callback);

    /**
     * Cancel the ongoing API call.
     */
    void cancel();
}
