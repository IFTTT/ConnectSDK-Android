package com.ifttt;

import java.io.IOException;
import javax.annotation.Nullable;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * OkHttp {@link Interceptor} for setting and clearing user authentication header.
 */
final class TokenInterceptor implements Interceptor {
    @Nullable private String token;

    void setToken(@Nullable String token) {
        this.token = token;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (token != null) {
            return chain.proceed(chain.request().newBuilder().addHeader("Authorization", "Bearer " + token).build());
        }
        return chain.proceed(chain.request());
    }
}
