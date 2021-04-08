package com.ifttt.location;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public final class CacheUserTokenProviderTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void shouldPreferDelegate() {
        UserTokenCache cache = new SharedPreferenceUserTokenCache(context);
        cache.store("cached");
        CacheUserTokenProvider provider = new CacheUserTokenProvider(cache, () -> "delegate");

        assertThat(provider.getUserToken()).isEqualTo("delegate");
    }

    @Test
    public void shouldReturnCachedForNullDelegate() {
        UserTokenCache cache = new SharedPreferenceUserTokenCache(context);
        cache.store("cached");
        CacheUserTokenProvider provider = new CacheUserTokenProvider(cache, null);

        assertThat(provider.getUserToken()).isEqualTo("cached");
    }

    @Test
    public void shouldCacheNonNullToken() {
        UserTokenCache cache = new SharedPreferenceUserTokenCache(context);
        CacheUserTokenProvider provider = new CacheUserTokenProvider(cache, () -> "delegate");
        provider.getUserToken();

        assertThat(cache.get()).isEqualTo("delegate");
    }

    @Test
    public void shouldNotCacheNullToken() {
        UserTokenCache cache = new SharedPreferenceUserTokenCache(context);
        CacheUserTokenProvider provider = new CacheUserTokenProvider(cache, () -> null);
        provider.getUserToken();

        assertThat(cache.get()).isNull();
    }
}
