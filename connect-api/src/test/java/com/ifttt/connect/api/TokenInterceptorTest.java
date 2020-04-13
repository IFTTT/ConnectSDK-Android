package com.ifttt.connect.api;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class TokenInterceptorTest {

    private final MockWebServer server = new MockWebServer();

    @Before
    public void setUp() throws Exception {
        server.start();
    }

    @Test
    public void shouldCallProviderForApiCalls() throws IOException {
        TokenInterceptor interceptor = new TokenInterceptor(() -> "token");

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        server.enqueue(new MockResponse());
        client.newCall(TokenInterceptorTest.this.request()).execute();
        assertThat(interceptor.isUserAuthenticated()).isTrue();
    }

    @Test
    public void shouldInvalidateTokenIf401() throws IOException {
        TokenInterceptor interceptor = new TokenInterceptor(() -> "token");

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        server.enqueue(new MockResponse());
        server.enqueue(new MockResponse().setResponseCode(401));
        client.newCall(request()).execute();
        assertThat(interceptor.isUserAuthenticated()).isTrue();

        client.newCall(request()).execute();
        assertThat(interceptor.isUserAuthenticated()).isFalse();
    }

    private Request request() {
        return new Request.Builder().url(server.url("")).build();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }
}
