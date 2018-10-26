package com.ifttt.api.demo.api

import com.ifttt.api.demo.api.TokenJsonAdapter.Token
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Query

interface TokenApi {
    @Token
    @POST("/mobile_api/log_in")
    fun login(@Query("username") username: String): Call<String>

    @Token
    @POST("/mobile_api/get_ifttt_token")
    fun getIftttToken(): Call<String?>
}
