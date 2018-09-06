package com.ifttt;

import com.ifttt.UserTokenJsonAdapter.UserTokenRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Retrofit interface for fetching authenticated user information.
 */
interface RetrofitUserApi {
    @GET("/v2/me")
    Call<User> user();

    @POST("/v2/user_token")
    Call<String> getUserToken(@UserTokenRequest @Body String token, @Header("IFTTT-Service-Key") String serviceKey);
}
