package com.ifttt;

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
 * Main class of the IFTTT API. There is a singleton instance provided in this class to do all API calls to
 * IFTTT API.
 */
public final class IftttApiClient {

    @Nullable private final String inviteCode;
    private final IftttApi iftttApi;
    private final TokenInterceptor tokenInterceptor;

    private IftttApiClient(@Nullable String inviteCode, RetrofitAppletsApi retrofitAppletsApi,
            RetrofitAppletConfigApi retrofitAppletConfigApi, RetrofitUserApi retrofitUserApi,
            JsonAdapter<ErrorResponse> errorResponseJsonAdapter, TokenInterceptor tokenInterceptor) {
        this.inviteCode = inviteCode;
        this.tokenInterceptor = tokenInterceptor;
        iftttApi = new IftttApiImpl(retrofitAppletsApi, retrofitUserApi, retrofitAppletConfigApi,
                errorResponseJsonAdapter);
    }

    public IftttApi api() {
        return iftttApi;
    }

    @Nullable
    public String getInviteCode() {
        return inviteCode;
    }

    @MainThread
    public void setUserToken(String userToken) {
        tokenInterceptor.setToken(userToken);
    }

    @MainThread
    @CheckReturnValue
    public boolean isUserAuthenticated() {
        return tokenInterceptor.isUserAuthenticated();
    }

    public static final class Builder {
        @Nullable private String inviteCode;

        public Builder setInviteCode(String inviteCode) {
            this.inviteCode = inviteCode;
            return this;
        }

        public IftttApiClient build() {
            Moshi moshi = new Moshi.Builder().add(new HexColorJsonAdapter())
                    .add(Date.class, new Rfc3339DateJsonAdapter().nullSafe())
                    .add(new AppletJsonAdapter())
                    .add(new UserTokenJsonAdapter())
                    .build();
            JsonAdapter<ErrorResponse> errorResponseJsonAdapter = moshi.adapter(ErrorResponse.class);
            TokenInterceptor tokenInterceptor = new TokenInterceptor(null);
            OkHttpClient.Builder builder = new OkHttpClient.Builder().addInterceptor(new SdkInfoInterceptor())
                    .addInterceptor(tokenInterceptor);

            if (inviteCode != null) {
                builder.addInterceptor(new InviteCodeInterceptor(inviteCode));
            }

            OkHttpClient okHttpClient = builder.build();
            Retrofit retrofit = new Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create(moshi))
                    .baseUrl("https://api.ifttt.com")
                    .client(okHttpClient)
                    .build();

            RetrofitAppletsApi retrofitAppletsApi = retrofit.create(RetrofitAppletsApi.class);
            RetrofitAppletConfigApi retrofitAppletConfigApi = retrofit.create(RetrofitAppletConfigApi.class);
            RetrofitUserApi retrofitUserApi = retrofit.create(RetrofitUserApi.class);

            return new IftttApiClient(inviteCode, retrofitAppletsApi, retrofitAppletConfigApi, retrofitUserApi,
                    errorResponseJsonAdapter, tokenInterceptor);
        }
    }

    private static final class IftttApiImpl implements IftttApi {

        private final RetrofitAppletsApi retrofitAppletsApi;
        private final RetrofitUserApi retrofitUserApi;
        private final RetrofitAppletConfigApi retrofitAppletConfigApi;
        private final JsonAdapter<ErrorResponse> errorResponseJsonAdapter;

        IftttApiImpl(RetrofitAppletsApi retrofitAppletsApi, RetrofitUserApi retrofitUserApi,
                RetrofitAppletConfigApi retrofitAppletConfigApi, JsonAdapter<ErrorResponse> errorResponseJsonAdapter) {
            this.retrofitAppletsApi = retrofitAppletsApi;
            this.retrofitUserApi = retrofitUserApi;
            this.retrofitAppletConfigApi = retrofitAppletConfigApi;
            this.errorResponseJsonAdapter = errorResponseJsonAdapter;
        }

        @Override
        public PendingResult<Applet> showApplet(String appletId) {
            return new ApiPendingResult<>(retrofitAppletsApi.showApplet(appletId), errorResponseJsonAdapter);
        }

        @Override
        public PendingResult<Applet> disableApplet(String appletId) {
            return new ApiPendingResult<>(retrofitAppletConfigApi.disableApplet(appletId), errorResponseJsonAdapter);
        }

        @Override
        public PendingResult<User> user() {
            return new ApiPendingResult<>(retrofitUserApi.user(), errorResponseJsonAdapter);
        }

        @Override
        public PendingResult<String> userToken(String oAuthToken, String serviceKey) {
            return new ApiPendingResult<>(retrofitUserApi.getUserToken(oAuthToken, serviceKey),
                    errorResponseJsonAdapter);
        }
    }
}
