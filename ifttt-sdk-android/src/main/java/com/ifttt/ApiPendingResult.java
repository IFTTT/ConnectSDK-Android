package com.ifttt;

import com.ifttt.api.PendingResult;
import com.squareup.moshi.JsonAdapter;
import java.io.IOException;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Implementation of the Retrofit {@link Call} wrapper.
 */
final class ApiPendingResult<T> implements PendingResult<T> {
    private static final ErrorResponse UNEXPECTED_ERROR = new ErrorResponse("exception", "Unexpected error");

    private final Call<T> originalCall;
    private final JsonAdapter<ErrorResponse> errorResponseJsonAdapter;

    ApiPendingResult(Call<T> originalCall, JsonAdapter<ErrorResponse> errorResponseJsonAdapter) {
        this.originalCall = originalCall;
        this.errorResponseJsonAdapter = errorResponseJsonAdapter;
    }

    @Override
    public Call<T> getCall() {
        return originalCall;
    }

    @Override
    public void execute(final ResultCallback<T> callback) {
        originalCall.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (!response.isSuccessful()) {
                    try {
                        ErrorResponse errorResponse = errorResponseJsonAdapter.fromJson(response.errorBody().source());
                        if (errorResponse == null) {
                            callback.onFailure(UNEXPECTED_ERROR);
                            return;
                        }

                        callback.onFailure(errorResponse);
                    } catch (IOException e) {
                        callback.onFailure(UNEXPECTED_ERROR);
                    }

                    return;
                }

                T result = response.body();
                if (result == null) {
                    callback.onFailure(UNEXPECTED_ERROR);
                    return;
                }

                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                callback.onFailure(UNEXPECTED_ERROR);
            }
        });
    }

    @Override
    public void cancel() {
        originalCall.cancel();
    }

    @Override
    public boolean isCanceled() {
        return originalCall.isCanceled();
    }
}
