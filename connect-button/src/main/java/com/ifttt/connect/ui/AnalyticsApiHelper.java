package com.ifttt.connect.ui;

import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

class AnalyticsApiHelper {

    private static AnalyticsApiHelper INSTANCE;

    private final EventsApi eventsApi;

    private AnalyticsApiHelper() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        OkHttpClient okHttpClient = builder.build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://connect.ifttt.com")
                .addConverterFactory(MoshiConverterFactory.create())
                .client(okHttpClient)
                .build();

        eventsApi = retrofit.create(EventsApi.class);
    }

    static AnalyticsApiHelper get() {
        if (INSTANCE == null) {
            INSTANCE = new AnalyticsApiHelper();
        }
        return INSTANCE;
    }

    Call<Void> submitEvents(List<Map<String, Object>> events) {
        return eventsApi.postEvents(events);
    }

    private interface EventsApi {
        @POST("/v2/sdk/events")
        Call<Void> postEvents(@Body List<Map<String, Object>> events);
    }
}
