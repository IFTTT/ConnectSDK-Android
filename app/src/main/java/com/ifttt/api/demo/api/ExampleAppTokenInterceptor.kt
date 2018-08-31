package com.ifttt.api.demo.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

internal class ExampleAppTokenInterceptor : Interceptor {
    private var exampleAppToken: String? = null

    fun setToken(exampleAppToken: String?) {
        this.exampleAppToken = exampleAppToken
    }

    @Throws(IOException::class) override fun intercept(chain: Interceptor.Chain): Response {
        return if (exampleAppToken != null) {
            chain.proceed(chain.request().newBuilder().addHeader("Authorization", "Bearer " + exampleAppToken!!).build())
        } else chain.proceed(chain.request())
    }
}
