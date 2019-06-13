package com.ifttt;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Connection API endpoints.
 */
interface RetrofitConnectionApi {

    @GET("/v2/connections/{id}")
    Call<Connection> showConnection(@Path("id") String id);

    @POST("/v2/connections/{id}/disable")
    Call<Connection> disableConnection(@Path("id") String id);

    @GET("/v2/me")
    Call<User> user();
}
