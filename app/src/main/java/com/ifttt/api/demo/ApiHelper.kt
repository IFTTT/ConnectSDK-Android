package com.ifttt.api.demo

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * This API helper represents your app's business logic with your backend server. We're using an example service here
 * to demonstrate the process of exchanging IFTTT user token.
 */
object ApiHelper {
    const val INVITE_CODE = "213621-90a10d229fbf8177a7ba0e6249847daf"
    const val REDIRECT_URI = "groceryexpress://connectcallback"
    const val SERVICE_ID = "grocery_express"

    interface Callback {
        fun onSuccess(token: String?)

        fun onFailure(code: String?)
    }

    private val tokenApi: TokenApi

    init {
        val client =
            OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()
        val moshi = Moshi.Builder().build()
        val retrofit = Retrofit.Builder().baseUrl("https://grocery-express.ifttt.com")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi)).build()
        tokenApi = retrofit.create(TokenApi::class.java)
    }

    /**
     * This method simulates the process of returning an IFTTT user token given a user's OAuth credential. This request
     * should happen between your application and your backend server, as it involves IFTTT service key.
     */
    fun getUserToken(oAuthCode: String, callback: Callback) {
        tokenApi.getUserToken(oAuthCode).enqueue(object : retrofit2.Callback<Token> {
            override fun onFailure(call: Call<Token>, t: Throwable) {
                callback.onFailure(null)
            }

            override fun onResponse(call: Call<Token>, response: Response<Token>) {
                if (!response.isSuccessful) {
                    callback.onFailure(null)
                    return
                }

                val token = response.body()!!
                if (token.code != null) {
                    callback.onFailure(token.code)
                } else {
                    callback.onSuccess(token.user_token)
                }
            }
        })
    }

    private interface TokenApi {
        @FormUrlEncoded
        @POST("/api/user_token")
        fun getUserToken(@Field("code") code: String): Call<Token>
    }

    private class Token(val type: String, val code: String?, val user_token: String?)
}
