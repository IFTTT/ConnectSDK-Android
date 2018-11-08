package com.ifttt;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import com.ifttt.ui.IftttConnectButton;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import okio.Okio;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public final class IftttConnectButtonTest {

    private final Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter().nullSafe())
            .add(new ConnectionJsonAdapter())
            .add(new TestHexColorJsonAdapter())
            .build();
    private final JsonAdapter<Connection> adapter = moshi.adapter(Connection.class);

    private IftttConnectButton button;

    @Before
    public void setUp() throws Exception {
        Activity activity = Robolectric.setupActivity(TestActivity.class);
        button = activity.findViewById(R.id.ifttt_connect_button_test);
    }

    @Test
    public void initButton() {
        TextView connectText = button.findViewById(R.id.connect_with_ifttt);
        assertThat(connectText.getText()).isEqualTo("");

        ImageView iconImage = button.findViewById(R.id.ifttt_icon);
        assertThat(iconImage.getBackground()).isNull();

        ViewGroup buttonRoot = button.findViewById(R.id.ifttt_toggle_root);
        assertThat(buttonRoot.getVisibility()).isEqualTo(View.VISIBLE);

        ViewGroup progressRoot = button.findViewById(R.id.ifttt_progress_container);
        assertThat(progressRoot.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(progressRoot.getAlpha()).isEqualTo(0f);

        TextSwitcher helperText = button.findViewById(R.id.ifttt_helper_text);
        assertThat(helperText.getCurrentView()).isInstanceOf(TextView.class);
        assertThat(((TextView) helperText.getCurrentView()).getText()).isEqualTo("");
    }

    @Test
    public void setConnection() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("connection.json");
        JsonReader jsonReader = JsonReader.of(Okio.buffer(Okio.source(inputStream)));
        Connection connection = adapter.fromJson(jsonReader);

        button.setConnection(connection);

        TextView connectText = button.findViewById(R.id.connect_with_ifttt);
        assertThat(connectText.getText()).isEqualTo("Connect Twitter");

        TextSwitcher helperText = button.findViewById(R.id.ifttt_helper_text);
        assertThat(helperText.getCurrentView()).isInstanceOf(TextView.class);
    }
}
