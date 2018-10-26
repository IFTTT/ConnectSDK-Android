package com.ifttt.ui;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonQualifier;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.ToJson;
import java.io.IOException;
import java.lang.annotation.Retention;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * API used for exchanging user OAuth token with an opaque token. The opaque token is going to be used for connecting
 * user service on IFTTT.
 */
interface TokenExchangeApi {
    @POST("/access/api/handshake.json")
    @TokenExchangeRequest
    Call<String> exchangeToken(@Body @TokenExchangeRequest String token);

    @Retention(RUNTIME)
    @JsonQualifier
    @interface TokenExchangeRequest {
    }

    Object TOKEN_EXCHANGE_JSON_ADAPTER = new Object() {

        private final JsonReader.Options options = JsonReader.Options.of("token");

        @FromJson
        @TokenExchangeRequest
        String fromJson(JsonReader reader) throws IOException {
            String token = null;
            reader.beginObject();
            while (reader.hasNext()) {
                int index = reader.selectName(options);
                if (index == 0) {
                    token = reader.nextString();
                } else {
                    reader.skipName();
                    reader.skipValue();
                }
            }
            reader.endObject();
            return token;
        }

        @ToJson
        void toJson(JsonWriter writer, @TokenExchangeRequest String token) throws IOException {
            writer.beginObject();
            writer.name("token").value(token);
            writer.endObject();
        }
    };
}
