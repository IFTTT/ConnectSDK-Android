package com.ifttt.connect.api;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class UserTokenJsonAdapterTest {

    JsonAdapter<String> adapter;

    @Before
    public void setUp() throws Exception {
        Moshi moshi = new Moshi.Builder().add(new UserTokenJsonAdapter()).build();
        adapter = moshi.adapter(String.class, UserTokenJsonAdapter.UserTokenRequest.class);
    }

    @Test
    public void toJson() {
        String value = adapter.toJson("my_token");
        assertThat(value).isEqualTo("{\"token\":\"my_token\"}");
    }
}
