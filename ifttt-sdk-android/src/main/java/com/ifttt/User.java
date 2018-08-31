package com.ifttt;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import com.ifttt.api.UserApi;
import com.squareup.moshi.Json;

/**
 * Data structure for a user from {@link UserApi#user()}.
 */
public final class User implements Parcelable {

    /**
     * Authentication scope of the API call. Currently the mobile SDK only support user token authentication or no
     * authentication.
     *
     * More information can be found in https://platform.ifttt.com/docs/ifttt_api#authentication.
     */
    public enum AuthenticationLevel {
        /**
         * A request made with no credentials. Requests at this level can only read publicly-visible information.
         */
        none,

        /**
         *  A request that includes an Authorization header containing a user-specific token that IFTTT has issued to
         *  your service.
         */
        user
    }

    @Json(name = "authentication_level") public final AuthenticationLevel authenticationLevel;
    /**
     * Service id on IFTTT Platform. Can be null if the API call is not authenticated.
     */
    @Json(name = "service_id") @Nullable public final String serviceId;

    /**
     * Username for the authenticated user on IFTTT. Can be null if the API call is not authenticated.
     */
    @Json(name = "user_login") @Nullable public final String userLogin;

    public User(AuthenticationLevel authenticationLevel, @Nullable String serviceId, @Nullable String userLogin) {
        this.authenticationLevel = authenticationLevel;
        this.serviceId = serviceId;
        this.userLogin = userLogin;
    }

    protected User(Parcel in) {
        authenticationLevel = AuthenticationLevel.valueOf(in.readString());
        serviceId = in.readString();
        userLogin = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(authenticationLevel.name());
        parcel.writeString(serviceId);
        parcel.writeString(userLogin);
    }
}
