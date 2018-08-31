package com.ifttt;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Rfc3339DateJsonAdapter;
import java.io.InputStream;
import java.util.Date;
import okio.Okio;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class AppletTest {

    private JsonAdapter<Applet> adapter;

    @Before
    public void setUp() throws Exception {
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter().nullSafe())
                .add(new TestHexColorJsonAdapter())
                .add(new AppletJsonAdapter())
                .build();
        adapter = moshi.adapter(Applet.class);
    }

    @Test
    public void testAppletDeserialization() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("applet.json");
        JsonReader jsonReader = JsonReader.of(Okio.buffer(Okio.source(inputStream)));
        Applet applet = adapter.fromJson(jsonReader);

        assertThat(applet).isNotNull();
        assertThat(applet.status).isNotNull();
        assertThat(applet.status).isEqualTo(Applet.Status.unknown);
        assertThat(applet.getPrimaryService().id).isEqualTo("instagram");
    }
}