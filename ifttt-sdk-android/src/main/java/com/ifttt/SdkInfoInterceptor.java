package com.ifttt;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Interceptor that adds common headers in the API call to IFTTT API.
 */
final class SdkInfoInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        return chain.proceed(chain.request()
                .newBuilder()
                .addHeader("IFTTT-SDK-Version", BuildConfig.VERSION_NAME)
                .addHeader("IFTTT-SDK-Platform", "android")
                .build());
    }
}
