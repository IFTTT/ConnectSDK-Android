package com.ifttt.ui;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * API used for trying to match an existing IFTTT account with an email.
 */
interface AccountApi {
    @GET("/v2/account/find")
    Call<Void> findAccount(@Query("email") String email);
}
