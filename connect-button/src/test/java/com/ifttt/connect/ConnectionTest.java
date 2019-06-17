package com.ifttt.connect;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;
import java.io.InputStream;
import java.util.Date;
import okio.Okio;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public final class ConnectionTest {

    private final Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter().nullSafe())
            .add(new HexColorJsonAdapter())
            .add(new ConnectionJsonAdapter())
            .build();
    private final JsonAdapter<Connection> adapter = moshi.adapter(Connection.class);

    private Connection connection;

    @Before
    public void setUp() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("connection.json");
        JsonReader jsonReader = JsonReader.of(Okio.buffer(Okio.source(inputStream)));
        connection = adapter.fromJson(jsonReader);
    }

    @Test
    public void testConnection() throws Exception {
        assertThat(connection.status).isNotNull();
        assertThat(connection.status).isEqualTo(Connection.Status.unknown);
    }

    @Test
    public void testPrimaryService() {
        assertThat(connection.getPrimaryService()).isNotNull();
        assertThat(connection.getPrimaryService().id).isEqualTo("instagram");
    }
}
