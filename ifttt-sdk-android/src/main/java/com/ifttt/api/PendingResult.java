package com.ifttt.api;

import android.support.annotation.NonNull;
import com.ifttt.ErrorResponse;
import retrofit2.Call;

/**
 * Wrapper interface for the Retrofit {@link Call}. It is used to provide standardized error response we have in the
 * API through {@link ErrorResponse}.
 */
public interface PendingResult<T> {

    interface ResultCallback<T> {
        void onSuccess(@NonNull T result);
        void onFailure(@NonNull ErrorResponse errorResponse);
    }

    /**
     * @return the wrapped Retrofit Call object. This can be used to access the additional functionality that Call
     * have, e.g. synchronous execution.
     */
    Call<T> getCall();
    void execute(ResultCallback<T> callback);
    void cancel();
    boolean isCanceled();
}
