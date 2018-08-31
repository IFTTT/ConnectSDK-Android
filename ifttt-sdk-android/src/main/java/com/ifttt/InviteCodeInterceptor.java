package com.ifttt;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * OkHttp {@link Interceptor} for setting and clearing invite code header.
 */
final class InviteCodeInterceptor implements Interceptor {
    @Nullable private String inviteCode;

    void setInviteCode(@Nullable String inviteCode) {
        this.inviteCode = inviteCode;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        if (inviteCode != null) {
            return chain.proceed(chain.request().newBuilder().addHeader("IFTTT-Invite-Code", inviteCode).build());
        }
        return chain.proceed(chain.request());
    }
}
