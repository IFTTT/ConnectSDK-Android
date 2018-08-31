package com.ifttt;

import com.ifttt.api.AppletsApi;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.ToJson;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Moshi JSON adapter for single Applet object from {@link AppletsApi#showApplet(String, String)}.
 */
final class AppletJsonAdapter {

    @FromJson
    Applet fromJson(AppletJson appletJson) throws IOException {
        Applet.Status status;
        if (appletJson.user_status == null) {
            status = Applet.Status.unknown;
        } else {
            status = Applet.Status.valueOf(appletJson.user_status);
        }

        return new Applet(appletJson.id, appletJson.name, appletJson.description, status, appletJson.published_at,
                appletJson.enabled_count, appletJson.last_run_at, appletJson.url, appletJson.embedded_url,
                appletJson.services);
    }

    @ToJson
    void toJson(JsonWriter jsonWriter, Applet applet) {
        throw new UnsupportedOperationException();
    }

    static final class AppletJson {
        final String id;
        final String name;
        final String description;
        final String user_status;
        final Date published_at;
        final int enabled_count;
        final Date last_run_at;
        final String url;
        final String embedded_url;
        final List<Service> services;

        AppletJson(String id, String name, String description, String user_status, Date published_at, int enabled_count,
                Date last_run_at, String url, String embedded_url, List<Service> services) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.user_status = user_status;
            this.published_at = published_at;
            this.enabled_count = enabled_count;
            this.last_run_at = last_run_at;
            this.url = url;
            this.embedded_url = embedded_url;
            this.services = services;
        }
    }
}
