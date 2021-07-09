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
        Cache<String> cache = new SharedPreferenceUserTokenCache(context);
        cache.write("cached");
        CacheUserTokenProvider provider = new CacheUserTokenProvider(cache, () -> "delegate");

        assertThat(provider.getUserToken()).isEqualTo("delegate");
    }

    @Test
    public void shouldReturnCachedForNullDelegate() {
        Cache<String> cache = new SharedPreferenceUserTokenCache(context);
        cache.write("cached");
        CacheUserTokenProvider provider = new CacheUserTokenProvider(cache, null);

        assertThat(provider.getUserToken()).isEqualTo("cached");
    }

    @Test
    public void shouldCacheNonNullToken() {
        Cache<String> cache = new SharedPreferenceUserTokenCache(context);
        CacheUserTokenProvider provider = new CacheUserTokenProvider(cache, () -> "delegate");
        provider.getUserToken();

        assertThat(cache.read()).isEqualTo("delegate");
    }

    @Test
    public void shouldNotCacheNullToken() {
        Cache<String> cache = new SharedPreferenceUserTokenCache(context);
        CacheUserTokenProvider provider = new CacheUserTokenProvider(cache, () -> null);
        provider.getUserToken();

        assertThat(cache.read()).isNull();
    }
}
