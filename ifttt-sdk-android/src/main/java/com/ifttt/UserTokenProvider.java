package com.ifttt;

import javax.annotation.Nullable;

public interface UserTokenProvider {
    interface Callback {
        void onTokenRetrieved(@Nullable String token);
    }

    void getUserToken(Callback callback);
}
