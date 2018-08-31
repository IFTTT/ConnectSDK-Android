package com.ifttt;

import android.support.annotation.NonNull;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

final class SdkInfoInterceptor implements Interceptor {

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        return chain.proceed(chain.request()
                .newBuilder()
                .addHeader("IFTTT-SDK-Version", BuildConfig.VERSION_NAME)
                .addHeader("IFTTT-SDK-Platform", "android")
                .addHeader("IFTTT-SDK-Anonymous-Id", IftttApiClient.ANONYMOUS_ID)
                .build());
    }
}
