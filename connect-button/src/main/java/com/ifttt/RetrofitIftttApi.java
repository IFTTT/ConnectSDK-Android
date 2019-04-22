package com.ifttt;

import com.ifttt.UserTokenJsonAdapter.UserTokenRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * IFTTT API endpoints.
 */
interface RetrofitIftttApi {

    @GET("/v2/connections/{id}")
    Call<Connection> showConnection(@Path("id") String id);

    @POST("/v2/connections/{id}/disable")
    Call<Connection> disableConnection(@Path("id") String id);

    @GET("/v2/me")
    Call<User> user();

    @POST("/v2/user_token")
    Call<String> getUserToken(@UserTokenRequest @Body String token, @Query("user_id") String userId,
            @Header("IFTTT-Service-Key") String serviceKey);
}
