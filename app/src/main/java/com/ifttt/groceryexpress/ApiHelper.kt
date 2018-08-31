package com.ifttt.groceryexpress

import android.net.Uri
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.io.IOException

/**
 * This API helper represents your app's business logic with your backend server. We're using an example service here
 * to demonstrate the process of exchanging IFTTT user token.
 */
object ApiHelper {
    val REDIRECT_URI: Uri = Uri.parse("groceryexpress://connectcallback")

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
    fun getUserToken(oAuthCode: String?): String? {
        if (oAuthCode == null) {
            return null
        }

        return try {
            val response = tokenApi.getUserToken(oAuthCode).execute()
            if (response.isSuccessful) {
                response.body()?.user_token
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    private interface TokenApi {
        @FormUrlEncoded
        @POST("/api/user_token")
        fun getUserToken(@Field("code") code: String): Call<Token>
    }

    private class Token(val type: String, val code: String?, val user_token: String?)
}
