package com.ifttt.location;

import java.util.List;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

interface RetrofitLocationApi {
    @POST("/v1/location_events")
    Call<Void> upload(@Body List<LocationInfo> locationInfoList);

    final class Client {

        final RetrofitLocationApi api;

        Client(Interceptor tokenInterceptor) {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(tokenInterceptor).build();
            Retrofit retrofit = new Retrofit.Builder().client(client)
                .baseUrl("https://connectapi.ifttt.com")
                .addConverterFactory(MoshiConverterFactory.create())
                .build();
            api = retrofit.create(RetrofitLocationApi.class);
        }
    }
}
