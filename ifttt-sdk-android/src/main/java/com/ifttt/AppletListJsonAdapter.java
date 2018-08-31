package com.ifttt;

import com.ifttt.api.AppletsApi;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.ToJson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Moshi JSON adapter for a list of Applets from {@link AppletsApi#listApplets(String, AppletsApi.Platform, AppletsApi.Order)} endpoint.
 */
final class AppletListJsonAdapter {
    private static final JsonReader.Options OPTIONS = JsonReader.Options.of("data");

    @FromJson
    List<Applet> fromJson(JsonReader jsonReader, JsonAdapter<Applet> delegate) throws IOException {
        List<Applet> applets = new ArrayList<>();
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            int index = jsonReader.selectName(OPTIONS);
            switch (index) {
                case -1:
                    jsonReader.skipValue();
                    break;
                case 0:
                    jsonReader.beginArray();
                    while (jsonReader.hasNext()) {
                        Applet applet = delegate.fromJson(jsonReader);
                        applets.add(applet);
                    }
                    jsonReader.endArray();
                    break;
                default:
                    throw new IllegalStateException("Unknown index: " + index);
            }
        }
        jsonReader.endObject();

        return applets;
    }

    @ToJson
    void toJson(JsonWriter jsonWriter, List<Applet> applets) {
        throw new UnsupportedOperationException();
    }
}
