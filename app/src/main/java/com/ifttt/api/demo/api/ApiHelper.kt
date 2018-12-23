package com.ifttt.api.demo.api

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * This API helper represents your app's business logic with your backend server. We're using an example service here
 * to demonstrate how to exchange IFTTT API token from the SDK, through your backend system, to IFTTT API.
 */
object ApiHelper {
    const val INVITE_CODE = "21790-7d53f29b1eaca0bdc5bd6ad24b8f4e1c"
    const val REDIRECT_URI = "ifttt-api-example://sdk-callback"
    const val SERVICE_ID = "ifttt_api_example"

    private val exampleAppTokenInterceptor = ExampleAppTokenInterceptor()
    private val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(exampleAppTokenInterceptor)
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()
    private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("http://ifttt-api-example.herokuapp.com")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder()
                    .add(TokenJsonAdapter)
                    .build()))
            .build()
    private val api: TokenApi = retrofit.create(TokenApi::class.java)

    /**
     * Log in the user with the user name, this simulates a user logging into your app. If the request returns
     * successfully, the example app token will be set to the interceptor, and all of the subsequent API calls will be
     * authenticated by it.
     */
    fun login(username: String, next: (String) -> Unit, error: () -> Unit) {
        api.login(username).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    val exampleAppToken = response.body()!!
                    exampleAppTokenInterceptor.setToken(exampleAppToken)
                    next(exampleAppToken)
                } else {
                    error()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                error()
            }
        })
    }

    /**
     * Retrieve a IFTTT API user token, this simulates your app requesting an IFTTT user token, in order to do user
     * authenticated calls to IFTTT API.
     */
    fun fetchIftttToken(next: (String?) -> Unit, error: () -> Unit) {
        api.getIftttToken().enqueue(object : Callback<String?> {
            override fun onFailure(call: Call<String?>, t: Throwable) {
                error()
            }

            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                if (response.isSuccessful) {
                    next(response.body())
                } else {
                    error()
                }
            }
        })
    }
}
