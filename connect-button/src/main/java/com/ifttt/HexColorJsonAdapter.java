package com.ifttt;

import android.graphics.Color;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.ToJson;
import java.io.IOException;

/**
 * Moshi JSON adapter for converting string representation of color values into integers.
 */
final class HexColorJsonAdapter {
    @ToJson
    void toJson(JsonWriter writer, @HexColor int color) throws IOException {
        throw new UnsupportedOperationException();
    }

    @FromJson
    @HexColor
    int fromJson(String value) {
        return Color.parseColor(value);
    }
}
