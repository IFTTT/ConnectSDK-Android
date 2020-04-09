package com.ifttt.connect.api;

import java.io.IOException;
import javax.annotation.Nullable;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * {@link Interceptor} for setting user authentication header.
 */
final class TokenInterceptor implements Interceptor {
    @Nullable private String token;
    @Nullable private UserTokenProvider credentialsProvider;

    TokenInterceptor(UserTokenProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    boolean isUserAuthenticated() {
        return token != null;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (token == null) {
            if (credentialsProvider != null) {
                try {
                    token = credentialsProvider.getUserToken();
                    if (token == null) {
                        // If token is still null, proceed without it.
                        return chain.proceed(chain.request());
                    }
                } catch (Exception e) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace();
                    }

                    return chain.proceed(chain.request());
                }
            } else {
                return chain.proceed(chain.request());
            }
        }

        Response response = chain.proceed(chain.request()
            .newBuilder()
            .addHeader("Authorization", "Bearer " + token)
            .build());

        if (response.code() == 401) {
            // If we are getting 401, invalid the cached token.
            token = null;
        }

        return response;
    }
}
