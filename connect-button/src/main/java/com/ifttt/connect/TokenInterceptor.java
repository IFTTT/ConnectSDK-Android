package com.ifttt.connect;

import java.io.IOException;
import javax.annotation.Nullable;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * {@link Interceptor} for setting user authentication header.
 */
final class TokenInterceptor implements Interceptor {
    @Nullable private String token;

    TokenInterceptor(@Nullable String token) {
        this.token = token;
    }

    void setToken(String token) {
        this.token = token;
    }

    boolean isUserAuthenticated() {
        return token != null;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (token == null) {
            return chain.proceed(chain.request());
        }

        return chain.proceed(chain.request().newBuilder().addHeader("Authorization", "Bearer " + token).build());
    }
}
