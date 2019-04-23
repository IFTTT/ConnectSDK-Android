package com.ifttt;

import com.ifttt.api.ConnectionApi;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.ToJson;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

/**
 * JSON adapter for single Connection object from {@link ConnectionApi#showConnection(String)}.
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

        return new Connection(connectionJson.id, connectionJson.name, connectionJson.description, status,
                connectionJson.url, connectionJson.services, connectionJson.cover_image,
                connectionJson.value_propositions);
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
        final String url;
        final List<Service> services;
        @Nullable final CoverImage cover_image;
        final List<ValueProposition> value_propositions;

        ConnectionJson(String id, String name, String description, @Nullable String user_status, Date published_at,
                String url, List<Service> services, @Nullable CoverImage cover_image,
                List<ValueProposition> value_propositions) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.user_status = user_status;
            this.published_at = published_at;
            this.url = url;
            this.services = services;
            this.cover_image = cover_image;
            this.value_propositions = value_propositions;
        }
    }
}
