package com.ifttt.connect.api;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import okio.Okio;

public final class TestUtils {

    private static final Moshi MOSHI = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter().nullSafe())
            .add(new ConnectionJsonAdapter())
            .add(new HexColorJsonAdapter())
            .build();

    private static final JsonAdapter<Connection> CONNECTION_ADAPTER = MOSHI.adapter(Connection.class);

    public static Connection loadConnection(ClassLoader classLoader) throws IOException {
        InputStream inputStream = classLoader.getResourceAsStream("connection.json");
        JsonReader jsonReader = JsonReader.of(Okio.buffer(Okio.source(inputStream)));
        return CONNECTION_ADAPTER.fromJson(jsonReader);
    }

    private TestUtils() {
        throw new AssertionError();
    }
}
