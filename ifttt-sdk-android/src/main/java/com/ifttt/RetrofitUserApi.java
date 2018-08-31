package com.ifttt;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Retrofit interface for fetching authenticated user information.
 */
interface RetrofitUserApi {
    @GET("/v1/me")
    Call<User> user();
}
