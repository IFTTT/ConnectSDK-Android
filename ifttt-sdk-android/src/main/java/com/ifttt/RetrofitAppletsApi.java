package com.ifttt;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Retrofit interface for showing Applets.
 */
interface RetrofitAppletsApi {

    @GET("/v2/applets/{applet_id}")
    Call<Applet> showApplet(@Path("applet_id") String appletId);
}
