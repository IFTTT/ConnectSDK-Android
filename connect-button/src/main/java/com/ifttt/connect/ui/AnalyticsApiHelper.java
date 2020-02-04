package com.ifttt.connect.ui;

import android.content.Context;
import com.ifttt.connect.BuildConfig;
import com.ifttt.connect.Factory;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

final class AnalyticsApiHelper {

    private static AnalyticsApiHelper INSTANCE;

    private final EventsApi eventsApi;

    private AnalyticsApiHelper(Context context) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        OkHttpClient okHttpClient = builder.addInterceptor(Factory.getApiInterceptor(AnalyticsPreferences.getAnonymousId(context))).build();

        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://connect.ifttt.com")
                .addConverterFactory(MoshiConverterFactory.create())
                .client(okHttpClient)
                .build();

        eventsApi = retrofit.create(EventsApi.class);
    }

    static synchronized AnalyticsApiHelper get(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AnalyticsApiHelper(context);
        }
        return INSTANCE;
    }

    Call<Void> submitEvents(EventsList events) {
        return eventsApi.postEvents(events);
    }

    private interface EventsApi {
        @POST("/v2/sdk/events")
        Call<Void> postEvents(@Body EventsList events);
    }
}
