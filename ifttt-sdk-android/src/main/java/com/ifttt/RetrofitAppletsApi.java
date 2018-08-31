package com.ifttt;

import com.ifttt.api.AppletsApi.Order;
import com.ifttt.api.AppletsApi.Platform;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface for showing Applets.
 */
interface RetrofitAppletsApi {

    @GET("/v1/services/{service_id}/applets")
    Call<List<Applet>> listApplets(@Path("service_id") String serviceId, @Query("platform") Platform platform,
            @Query("order") Order order);

    @GET("/v1/services/{service_id}/applets/{applet_id}")
    Call<Applet> showApplet(@Path("service_id") String serviceId, @Path("applet_id") String appletId);
}
