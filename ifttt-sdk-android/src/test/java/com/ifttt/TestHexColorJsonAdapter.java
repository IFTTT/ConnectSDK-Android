package com.ifttt;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.ToJson;
import java.io.IOException;

final class TestHexColorJsonAdapter {

    @ToJson
    void toJson(JsonWriter writer, @HexColor int color) throws IOException {
        throw new UnsupportedOperationException();
    }

    @FromJson
    @HexColor
    int fromJson(String value) {
        return 0;
    }
}
