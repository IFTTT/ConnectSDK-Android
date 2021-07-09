package com.ifttt.connect.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;
import java.util.Date;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * A wrapper class for IFTTT API. It exposes the API wrapper {@link ConnectionApi}, as well as providing a way to set the
 * user token.
 *
 * You can use a {@link Builder} to get an instance of ConnectionApiClient.
 */
public final class ConnectionApiClient {

    private final ConnectionApi connectionApi;
    private final TokenInterceptor tokenInterceptor;

    private final String anonymousId;
    private final String inviteCode;

    public final UserTokenProvider userTokenProvider;

    private ConnectionApiClient(
        RetrofitConnectionApi retrofitConnectionApi,
        JsonAdapter<ErrorResponse> errorResponseJsonAdapter,
        TokenInterceptor tokenInterceptor,
        String anonymousId,
        String inviteCode,
        UserTokenProvider userTokenProvider
    ) {
        this.tokenInterceptor = tokenInterceptor;
        this.anonymousId = anonymousId;
        this.inviteCode = inviteCode;
        this.userTokenProvider = userTokenProvider;
        connectionApi = new ConnectionApiImpl(retrofitConnectionApi, errorResponseJsonAdapter);
    }

    /**
     * @return Instance of the IFTTT API wrapper. You can use the instance to make various API calls.
     */
    public ConnectionApi api() {
        return connectionApi;
    }

    /**
     * @return true if this instance of ConnectionApiClient has a user token, or false otherwise.
     */
    @CheckReturnValue
    public boolean isUserAuthorized() {
        return tokenInterceptor.isUserAuthenticated();
    }

    /**
     * @return the {@link Interceptor} implementation for this API client.
     */
    @CheckReturnValue
    public Interceptor interceptor() {
        return tokenInterceptor;
    }

    /**
     * @param newUserTokenProvider a new {@link UserTokenProvider} instance to replace with the current one.
     *
     * @return a new {@link Builder} instance with existing ConnectionApiClient's configuration and the new
     * UserTokenProvider if not null.
     */
    public Builder newBuilder(@Nullable UserTokenProvider newUserTokenProvider) {
        UserTokenProvider userTokenProvider = this.userTokenProvider;
        if (newUserTokenProvider != null) {
            userTokenProvider = newUserTokenProvider;
        }

        return new Builder(anonymousId, userTokenProvider).setInviteCode(inviteCode);
    }

    /**
     * Builder class to get an {@link ConnectionApiClient} instance.
     */
    public static final class Builder {

        private final String anonymousId;

        @Nullable private String inviteCode;

        private final UserTokenProvider userTokenProvider;

        /**
         * @param context Context instance used to generate an anonymous id using the device's {@link Settings.Secure#ANDROID_ID}.
         * The value will be sent to IFTTT API and web view redirects for measurement and analytics purpose.
         */
        @SuppressLint("HardwareIds")
        public Builder(Context context, UserTokenProvider userTokenProvider) {
            this(AnonymousId.get(context), userTokenProvider);
        }

        Builder(String anonymousId, UserTokenProvider userTokenProvider) {
            this.anonymousId = anonymousId;
            this.userTokenProvider = userTokenProvider;
        }

        /**
         * Pass in a non-null String as the invite code for accessing an IFTTT service that has not yet published. You
         * may find this value on https://platform.ifttt.com.
         *
         * @param inviteCode An invite code String, cannot be null.
         */
        public Builder setInviteCode(String inviteCode) {
            this.inviteCode = inviteCode;
            return this;
        }

        public ConnectionApiClient build() {
            return buildWithBaseUrl("https://connect.ifttt.com");
        }

        ConnectionApiClient buildWithBaseUrl(String baseUrl) {
            Moshi moshi = new Moshi.Builder().add(new HexColorJsonAdapter())
                .add(Date.class, new Rfc3339DateJsonAdapter().nullSafe())
                .add(new ConnectionJsonAdapter())
                .add(new UserTokenJsonAdapter())
                .build();
            JsonAdapter<ErrorResponse> errorResponseJsonAdapter = moshi.adapter(ErrorResponse.class);
            TokenInterceptor tokenInterceptor = new TokenInterceptor(userTokenProvider);
            OkHttpClient.Builder builder
                = new OkHttpClient.Builder().addInterceptor(new SdkInfoInterceptor(anonymousId)).addInterceptor(
                tokenInterceptor);

            if (inviteCode != null) {
                builder.addInterceptor(new InviteCodeInterceptor(inviteCode));
            }

            OkHttpClient okHttpClient = builder.build();
            Retrofit retrofit = new Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create(moshi)).baseUrl(
                baseUrl).client(okHttpClient).build();

            RetrofitConnectionApi retrofitConnectionApi = retrofit.create(RetrofitConnectionApi.class);

            return new ConnectionApiClient(retrofitConnectionApi,
                errorResponseJsonAdapter,
                tokenInterceptor,
                anonymousId,
                inviteCode,
                userTokenProvider
            );
        }
    }

    private static final class ConnectionApiImpl implements ConnectionApi {

        private final RetrofitConnectionApi retrofitConnectionApi;
        private final JsonAdapter<ErrorResponse> errorResponseJsonAdapter;

        ConnectionApiImpl(
            RetrofitConnectionApi retrofitConnectionApi, JsonAdapter<ErrorResponse> errorResponseJsonAdapter
        ) {
            this.retrofitConnectionApi = retrofitConnectionApi;
            this.errorResponseJsonAdapter = errorResponseJsonAdapter;
        }

        @Override
        public PendingResult<Connection> showConnection(String id) {
            return new ApiPendingResult<>(retrofitConnectionApi.showConnection(id), errorResponseJsonAdapter);
        }

        @Override
        public PendingResult<Connection> disableConnection(String id) {
            return new ApiPendingResult<>(retrofitConnectionApi.disableConnection(id), errorResponseJsonAdapter);
        }

        @Override
        public PendingResult<Connection> reenableConnection(String id) {
            return new ApiPendingResult<>(retrofitConnectionApi.reenableConnection(id), errorResponseJsonAdapter);
        }

        @Override
        public PendingResult<User> user() {
            return new ApiPendingResult<>(retrofitConnectionApi.user(), errorResponseJsonAdapter);
        }
    }
}
