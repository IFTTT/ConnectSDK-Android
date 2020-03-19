package com.ifttt.connect;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Interceptor that adds common headers in the API call to IFTTT API.
 */
public final class SdkInfoInterceptor implements Interceptor {

    private final String anonymousId;

    public SdkInfoInterceptor(String anonymousId) {
        this.anonymousId = anonymousId;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        return chain.proceed(chain.request()
                .newBuilder()
                .addHeader("IFTTT-SDK-Version", BuildConfig.VERSION_NAME)
                .addHeader("IFTTT-SDK-Platform", "android")
                .addHeader("IFTTT-SDK-Anonymous-Id", anonymousId)
                .build());
    }
}
