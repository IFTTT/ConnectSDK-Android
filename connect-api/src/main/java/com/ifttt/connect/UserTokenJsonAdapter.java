package com.ifttt.connect;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonQualifier;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.ToJson;
import java.io.IOException;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * JSON adapter for parsing user token.
 */
final class UserTokenJsonAdapter {
    @Retention(RUNTIME)
    @JsonQualifier
    @interface UserTokenRequest {
    }

    @FromJson
    @UserTokenRequest
    String fromJson(JsonReader reader) throws IOException {
        throw new UnsupportedOperationException();
    }

    @ToJson
    void toJson(JsonWriter writer, @UserTokenRequest String token) throws IOException {
        writer.beginObject();
        writer.name("token").value(token);
        writer.endObject();
    }
}
