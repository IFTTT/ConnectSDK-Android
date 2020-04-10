package com.ifttt.connect.api;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * {@link Interceptor} for setting user authentication header.
 */
final class TokenInterceptor implements Interceptor {
    private final UserTokenProvider credentialsProvider;

    private boolean isUserAuthenticated = false;

    TokenInterceptor(UserTokenProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    /**
     * @return true if the Interceptor has a valid user token, false otherwise. This is represented as whether the
     * most recent {@link UserTokenProvider#getUserToken()} returned a non-null value and the API response did not have
     * a 401 status code.
     */
    boolean isUserAuthenticated() {
        return isUserAuthenticated;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String token;
        try {
            token = credentialsProvider.getUserToken();
            if (token == null) {
                // If token is still null, proceed without it.
                isUserAuthenticated = false;
                return chain.proceed(chain.request());
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }

            isUserAuthenticated = false;
            return chain.proceed(chain.request());
        }

        Response response = chain.proceed(chain.request()
            .newBuilder()
            .addHeader("Authorization", "Bearer " + token)
            .build());

        // If we are getting 401, reset the user authentication flag.
        isUserAuthenticated = response.code() != 401;

        return response;
    }
}
