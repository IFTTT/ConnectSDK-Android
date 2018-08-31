package com.ifttt.connect;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * OkHttp {@link Interceptor} for setting and clearing invite code header.
 */
final class InviteCodeInterceptor implements Interceptor {
    private final String inviteCode;

    InviteCodeInterceptor(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        return chain.proceed(chain.request().newBuilder().addHeader("IFTTT-Invite-Code", inviteCode).build());
    }
}
