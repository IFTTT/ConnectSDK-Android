package com.ifttt;

import android.support.annotation.NonNull;
import com.ifttt.api.AppletConfigApi;
import com.ifttt.api.AppletsApi;
import com.ifttt.api.PendingResult;
import com.ifttt.api.UserApi;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Rfc3339DateJsonAdapter;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * Main class of the IFTTT API. There is a singleton instance provided in this class to do all API calls to
 * IFTTT API. Additionally, you can modify the authentication state through {@link #setUserToken(String)}, as
 * well as setting invite code through {@link #setInviteCode(String)}, in order to access the Applets information for
 * preview services.
 *
 * To start, call {@link #getInstance()} to get the singleton instance of this class. Once you have the instance, call
 * the following methods to access the APIs you want to use:
 * <ul>
 *     <li>{@link #appletConfigApi()}: APIs for enabling and disabling an Applet.</li>
 *     <li>{@link #appletsApi()}: APIs for listing Applets or a single Applet from a service.</li>
 *     <li>{@link #userApi()}: API for retrieving IFTTT service and account information for the authenticated user.</li>
 * </ul>
 *
 */
public final class IftttApiClient {

    static final String ANONYMOUS_ID = UUID.randomUUID().toString();

    private static IftttApiClient INSTANCE;

    private final TokenInterceptor tokenInterceptor;
    private final InviteCodeInterceptor inviteCodeInterceptor;

    private final AppletsApi appletsApi;
    private final AppletConfigApi appletConfigApi;
    private final UserApi userApi;

    private IftttApiClient() {
        Moshi moshi = new Moshi.Builder().add(new HexColorJsonAdapter())
                .add(Date.class, new Rfc3339DateJsonAdapter().nullSafe())
                .add(new AppletJsonAdapter())
                .add(new AppletListJsonAdapter())
                .build();
        JsonAdapter<ErrorResponse> errorResponseJsonAdapter = moshi.adapter(ErrorResponse.class);

        tokenInterceptor = new TokenInterceptor();
        inviteCodeInterceptor = new InviteCodeInterceptor();
        OkHttpClient okHttpClient = new OkHttpClient.Builder().addInterceptor(tokenInterceptor)
                .addInterceptor(inviteCodeInterceptor)
                .addInterceptor(new SdkInfoInterceptor())
                .build();
        Retrofit retrofit = new Retrofit.Builder().addConverterFactory(MoshiConverterFactory.create(moshi))
                .baseUrl("https://api.ifttt.com")
                .client(okHttpClient)
                .build();

        RetrofitAppletConfigApi retrofitAppletConfigApi = retrofit.create(RetrofitAppletConfigApi.class);
        appletConfigApi = new InternalAppletConfigApi(retrofitAppletConfigApi, errorResponseJsonAdapter);

        RetrofitAppletsApi retrofitAppletsApi = retrofit.create(RetrofitAppletsApi.class);
        appletsApi = new InternalAppletsApi(retrofitAppletsApi, errorResponseJsonAdapter);

        RetrofitUserApi retrofitUserApi = retrofit.create(RetrofitUserApi.class);
        userApi = new InternalUserApi(retrofitUserApi, errorResponseJsonAdapter);
    }

    /**
     * @return API object for enabling and disabling an Applet.
     */
    public AppletConfigApi appletConfigApi() {
        return appletConfigApi;
    }

    /**
     * @return API object for listing Applets or a single Applet from a service.
     */
    public AppletsApi appletsApi() {
        return appletsApi;
    }

    /**
     * @return API object for retrieving IFTTT service and account information for the authenticated user.
     */
    public UserApi userApi() {
        return userApi;
    }

    /**
     * Once the user token for the API is retrieved, modify the interceptor to include auth header to all of the
     * requests.
     *
     * @param userToken User token string for IFTTT API.
     */
    public void setUserToken(@NonNull String userToken) {
        tokenInterceptor.setToken(userToken);
    }

    /**
     * Remove user token stored in this instance.
     */
    public void clearUserToken() {
        tokenInterceptor.setToken(null);
    }

    /**
     * For unpublished services, in order to access the service, an invite code can be included in the request headers.
     *
     * @param inviteCode Invite code string.
     */
    public void setInviteCode(@NonNull String inviteCode) {
        inviteCodeInterceptor.setInviteCode(inviteCode);
    }

    /**
     * Remove the invite code stored in this instance.
     */
    public void clearInviteCode() {
        inviteCodeInterceptor.setInviteCode(null);
    }

    public static IftttApiClient getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new IftttApiClient();
        }

        return INSTANCE;
    }

    private static final class InternalAppletsApi implements AppletsApi {

        private final RetrofitAppletsApi retrofitAppletsApi;
        private final JsonAdapter<ErrorResponse> errorResponseJsonAdapter;

        InternalAppletsApi(RetrofitAppletsApi retrofitAppletsApi, JsonAdapter<ErrorResponse> errorResponseJsonAdapter) {
            this.retrofitAppletsApi = retrofitAppletsApi;
            this.errorResponseJsonAdapter = errorResponseJsonAdapter;
        }

        public PendingResult<List<Applet>> listApplets(final String serviceId, final Platform platform,
                final Order order) {
            return new ApiPendingResult<>(retrofitAppletsApi.listApplets(serviceId, platform, order),
                    errorResponseJsonAdapter);
        }

        @Override
        public PendingResult<Applet> showApplet(String serviceId, String appletId) {
            return new ApiPendingResult<>(retrofitAppletsApi.showApplet(serviceId, appletId), errorResponseJsonAdapter);
        }
    }

    private static final class InternalAppletConfigApi implements AppletConfigApi {

        private final RetrofitAppletConfigApi retrofitAppletConfigApi;
        private final JsonAdapter<ErrorResponse> errorResponseJsonAdapter;

        InternalAppletConfigApi(RetrofitAppletConfigApi retrofitAppletConfigApi,
                JsonAdapter<ErrorResponse> errorResponseJsonAdapter) {
            this.retrofitAppletConfigApi = retrofitAppletConfigApi;
            this.errorResponseJsonAdapter = errorResponseJsonAdapter;
        }

        @Override
        public PendingResult<Applet> enableApplet(String serviceId, String appletId) {
            return new ApiPendingResult<>(retrofitAppletConfigApi.enableApplet(serviceId, appletId),
                    errorResponseJsonAdapter);
        }

        @Override
        public PendingResult<Applet> disableApplet(String serviceId, String appletId) {
            return new ApiPendingResult<>(retrofitAppletConfigApi.disableApplet(serviceId, appletId),
                    errorResponseJsonAdapter);
        }
    }

    private static final class InternalUserApi implements UserApi {
        private final RetrofitUserApi retrofitUserApi;
        private final JsonAdapter<ErrorResponse> errorResponseJsonAdapter;

        InternalUserApi(RetrofitUserApi retrofitUserApi, JsonAdapter<ErrorResponse> errorResponseJsonAdapter) {
            this.retrofitUserApi = retrofitUserApi;
            this.errorResponseJsonAdapter = errorResponseJsonAdapter;
        }

        @Override
        public PendingResult<User> user() {
            return new ApiPendingResult<>(retrofitUserApi.user(), errorResponseJsonAdapter);
        }
    }
}
