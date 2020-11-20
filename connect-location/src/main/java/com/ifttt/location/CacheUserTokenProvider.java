package com.ifttt.location;

import androidx.annotation.Nullable;
import com.ifttt.connect.api.UserTokenProvider;

class CacheUserTokenProvider implements UserTokenProvider {

    private final UserTokenCache cache;
    @Nullable private final UserTokenProvider delegate;

    CacheUserTokenProvider(UserTokenCache cache, @Nullable UserTokenProvider delegate) {
        this.cache = cache;
        this.delegate = delegate;
    }

    @Nullable
    @Override
    public String getUserToken() {
        String token;
        if (delegate != null) {
            token = delegate.getUserToken();
            cache.store(token);
        } else {
            token = cache.get();
        }

        return token;
    }
}
