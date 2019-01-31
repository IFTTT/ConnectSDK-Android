package com.ifttt.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ifttt.Connection;
import com.ifttt.IftttApiClient;
import com.ifttt.R;
import com.ifttt.ShadowResourcesCompat;
import com.ifttt.TestActivity;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static com.ifttt.TestUtils.loadConnection;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@Config(shadows = ShadowResourcesCompat.class)
public final class IftttConnectButtonTest {

    private IftttConnectButton button;

    @Before
    public void setUp() throws Exception {
        Activity activity = Robolectric.setupActivity(TestActivity.class);
        button = activity.findViewById(R.id.ifttt_connect_button_test);
    }

    @Test
    public void initButton() {
        ServiceNameTextView connectText = button.findViewById(R.id.connect_with_ifttt);
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
        assertThat(((TextView) helperText.getCurrentView()).getText().toString()).isEqualTo("");
    }

    @Test(expected = IllegalStateException.class)
    public void testWithoutSetup() throws Exception {
        Connection connection = loadConnection(getClass().getClassLoader());
        button.setConnection(connection);
        fail();
    }

    @Test
    public void setConnection() throws IOException {
        Connection connection = loadConnection(getClass().getClassLoader());

        button.setup("a@b.com", "instagram", new IftttApiClient.Builder().build(), "", () -> "");
        button.setConnection(connection);

        ServiceNameTextView connectText = button.findViewById(R.id.connect_with_ifttt);
        assertThat(connectText.getText()).isEqualTo("Connect Twitter");

        TextSwitcher helperText = button.findViewById(R.id.ifttt_helper_text);
        assertThat(helperText.getCurrentView()).isInstanceOf(TextView.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testOwnerServiceCheck() throws IOException {
        Connection connection = loadConnection(getClass().getClassLoader());

        button.setup("a@b.com", "not_owner_service", new IftttApiClient.Builder().build(), "", () -> "");
        button.setConnection(connection);

        fail();
    }
}
