package com.ifttt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import androidx.annotation.MainThread;
import com.ifttt.api.IftttApi;
import com.ifttt.api.PendingResult;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;
import java.util.Date;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * A wrapper class for IFTTT API. It exposes the API wrapper {@link IftttApi}, as well as providing a way to set the
 * user token.
 *
 * You can use a {@link Builder} to get an instance of IftttApiClient.
 */
public final class IftttApiClient {

    @Nullable private final String inviteCode;
    private final IftttApi iftttApi;
    private final TokenInterceptor tokenInterceptor;

    private IftttApiClient(@Nullable String inviteCode, RetrofitIftttApi retrofitIftttApi,
            JsonAdapter<ErrorResponse> errorResponseJsonAdapter, TokenInterceptor tokenInterceptor) {
        this.inviteCode = inviteCode;
        this.tokenInterceptor = tokenInterceptor;
        iftttApi = new IftttApiImpl(retrofitIftttApi, errorResponseJsonAdapter);
    }

    /**
     * @return Instance of the IFTTT API wrapper. You can use the instance to make various API calls.
     */
    public IftttApi api() {
        return iftttApi;
    }

    /**
     * @return The invite code that is set through {@link Builder#setInviteCode(String)}.
     */
    @Nullable
    public String getInviteCode() {
        return inviteCode;
    }

    /**
     * Pass in a non-null String as the user token. A user token may be used to make API calls to IFTTT API, so that
     * the response will contain user-specific information.
     *
     * After setting the user token, the same IftttApiClient will start using it in all of the subsequent API calls.
     *
     * @param userToken A user token String, cannot be null.
     */
    @MainThread
    public void setUserToken(String userToken) {
        tokenInterceptor.setToken(userToken);
    }

    /**
     * @return true if this instance of IftttApiClient has a user token, or false otherwise.
     */
    @MainThread
    @CheckReturnValue
    public boolean isUserAuthenticated() {
        return tokenInterceptor.isUserAuthenticated();
    }

    /**
     * Builder class to get an {@link IftttApiClient} instance.
     */
    public static final class Builder {

        private final String anonymousId;

        @Nullable private String inviteCode;

        /**
         * @param context Context instance used to generate an anonymous id using the device's {@link Settings.Secure#ANDROID_ID}.
         * The value will be sent to IFTTT API and web view redirects for measurement and analytics purpose.
         */
        @SuppressLint("HardwareIds")
        public Builder(Context context) {
            anonymousId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
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

        public IftttApiClient build() {
            Moshi moshi = new Moshi.Builder().add(new HexColorJsonAdapter())
                    .add(Date.class, new Rfc3339DateJsonAdapter().nullSafe())
                    .add(new ConnectionJsonAdapter())
                    .add(new UserTokenJsonAdapter())
                    .build();
            JsonAdapter<ErrorResponse> errorResponseJsonAdapter = moshi.adapter(ErrorResponse.class);
            TokenInterceptor tokenInterceptor = new TokenInterceptor(null);
            OkHttpClient.Builder builder =
                    new OkHttpClient.Builder().addInterceptor(new SdkInfoInterceptor(anonymousId))
                            .addInterceptor(tokenInterceptor);

            if (inviteCode != null) {
                builder.addInterceptor(new InviteCodeInterceptor(inviteCode));
            }

            OkHttpClient okHttpClient = builder.build();
            Retrofit retrofit = new Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create(moshi))
                    .baseUrl("https://api.ifttt.com")
                    .client(okHttpClient)
                    .build();

            RetrofitIftttApi retrofitIftttApi = retrofit.create(RetrofitIftttApi.class);

            return new IftttApiClient(inviteCode, retrofitIftttApi, errorResponseJsonAdapter, tokenInterceptor);
        }
    }

    private static final class IftttApiImpl implements IftttApi {

        private final RetrofitIftttApi retrofitIftttApi;
        private final JsonAdapter<ErrorResponse> errorResponseJsonAdapter;

        IftttApiImpl(RetrofitIftttApi retrofitIftttApi, JsonAdapter<ErrorResponse> errorResponseJsonAdapter) {
            this.retrofitIftttApi = retrofitIftttApi;
            this.errorResponseJsonAdapter = errorResponseJsonAdapter;
        }

        @Override
        public PendingResult<Connection> showConnection(String id) {
            return new ApiPendingResult<>(retrofitIftttApi.showConnection(id), errorResponseJsonAdapter);
        }

        @Override
        public PendingResult<Connection> disableConnection(String id) {
            return new ApiPendingResult<>(retrofitIftttApi.disableConnection(id), errorResponseJsonAdapter);
        }

        @Override
        public PendingResult<User> user() {
            return new ApiPendingResult<>(retrofitIftttApi.user(), errorResponseJsonAdapter);
        }

        @Override
        public PendingResult<String> userToken(String oAuthToken, String userId, String serviceKey) {
            return new ApiPendingResult<>(retrofitIftttApi.getUserToken(oAuthToken, userId, serviceKey),
                    errorResponseJsonAdapter);
        }
    }
}
