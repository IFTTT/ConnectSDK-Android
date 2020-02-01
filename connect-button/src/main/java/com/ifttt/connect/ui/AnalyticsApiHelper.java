package com.ifttt.connect.ui;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

final class AnalyticsApiHelper {

    private static AnalyticsApiHelper INSTANCE;

    private final EventsApi eventsApi;

    private AnalyticsApiHelper() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // TODO: Remove interceptor before merging
        OkHttpClient okHttpClient = builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)).build();

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

    Call<Void> submitEvents(String anonymousId, EventsList events) {
        // TODO: Remove service key header from the api before merging
        return eventsApi.postEvents(anonymousId, events);
    }

    private interface EventsApi {
        @Headers("Content-Type: application/json")
        @POST("/v2/sdk/events")
        Call<Void> postEvents(@Header("IFTTT-Service-Key") String anonymousId, @Body EventsList events);
    }
}
