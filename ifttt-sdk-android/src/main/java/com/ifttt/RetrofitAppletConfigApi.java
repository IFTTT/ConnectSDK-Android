package com.ifttt;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit interface for enabling and disabling Applets.
 */
interface RetrofitAppletConfigApi {
    @POST("/v1/services/{service_id}/applets/{applet_id}/enable")
    Call<Applet> enableApplet(@Path("service_id") String serviceId, @Path("applet_id") String appletId);

    @POST("/v1/services/{service_id}/applets/{applet_id}/disable")
    Call<Applet> disableApplet(@Path("service_id") String serviceId, @Path("applet_id") String appletId);
}
