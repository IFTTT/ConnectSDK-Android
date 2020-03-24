package com.ifttt.connect;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class TokenInterceptorTest {

    @Test
    public void nullToken() {
        TokenInterceptor tokenInterceptor = new TokenInterceptor(null);
        assertThat(tokenInterceptor.isUserAuthenticated()).isFalse();
    }

    @Test
    public void nonNullToken() {
        TokenInterceptor tokenInterceptor = new TokenInterceptor("Token");
        assertThat(tokenInterceptor.isUserAuthenticated()).isTrue();
    }

    @Test
    public void setToken() {
        TokenInterceptor tokenInterceptor = new TokenInterceptor(null);
        tokenInterceptor.setToken("Token");
        assertThat(tokenInterceptor.isUserAuthenticated()).isTrue();
    }
}
