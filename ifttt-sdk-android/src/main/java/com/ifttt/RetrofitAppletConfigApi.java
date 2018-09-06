package com.ifttt;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit interface for disabling Applets.
 */
interface RetrofitAppletConfigApi {
    @POST("/v2/applets/{applet_id}/disable")
    Call<Applet> disableApplet(@Path("applet_id") String appletId);
}
