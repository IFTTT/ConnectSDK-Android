package com.ifttt;

import com.ifttt.api.IftttApi;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.ToJson;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

/**
 * JSON adapter for single Connection object from {@link IftttApi#showConnection(String)}.
 */
final class ConnectionJsonAdapter {

    @FromJson
    Connection fromJson(ConnectionJson connectionJson) throws IOException {
        Connection.Status status;
        if (connectionJson.user_status == null) {
            status = Connection.Status.unknown;
        } else {
            status = Connection.Status.valueOf(connectionJson.user_status);
        }

        return new Connection(
                connectionJson.id, connectionJson.name, connectionJson.description, status, connectionJson.published_at,
                connectionJson.enabled_count, connectionJson.last_run_at, connectionJson.url, connectionJson.embedded_url,
                connectionJson.services);
    }

    @ToJson
    void toJson(JsonWriter jsonWriter, Connection connection) {
        throw new UnsupportedOperationException();
    }

    static final class ConnectionJson {
        final String id;
        final String name;
        final String description;
        @Nullable final String user_status;
        final Date published_at;
        final int enabled_count;
        final Date last_run_at;
        final String url;
        final String embedded_url;
        final List<Service> services;

        ConnectionJson(String id, String name, String description, @Nullable String user_status, Date published_at,
                int enabled_count, Date last_run_at, String url, String embedded_url, List<Service> services) {
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
