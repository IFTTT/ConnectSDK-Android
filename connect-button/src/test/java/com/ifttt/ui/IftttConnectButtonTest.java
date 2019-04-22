package com.ifttt.ui;

import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ifttt.Connection;
import com.ifttt.ErrorResponse;
import com.ifttt.IftttApiClient;
import com.ifttt.R;
import com.ifttt.TestActivity;
import com.ifttt.ui.IftttConnectButton.ButtonState;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.truth.Truth.assertThat;
import static com.ifttt.TestUtils.loadConnection;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public final class IftttConnectButtonTest {

    private IftttConnectButton button;
    private IftttApiClient client;

    @Before
    public void setUp() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);
        scenario.onActivity(activity -> {
            button = activity.findViewById(R.id.ifttt_connect_button_test);
            client = new IftttApiClient.Builder(activity).build();
        });
    }

    @Test
    public void initButton() {
        TextSwitcher connectText = button.findViewById(R.id.connect_with_ifttt);
        assertThat(connectText.getCurrentView()).isInstanceOf(TextView.class);
        assertThat(((TextView) connectText.getCurrentView()).getText()).isEqualTo("");

        ImageView iconImage = button.findViewById(R.id.ifttt_icon);
        assertThat(iconImage.getBackground()).isNull();

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

        button.setup("a@b.com", "instagram", client, "", () -> "");
        button.setConnection(connection);

        TextSwitcher connectText = button.findViewById(R.id.connect_with_ifttt);
        assertThat(connectText.getCurrentView()).isInstanceOf(TextView.class);
        assertThat(((TextView) connectText.getCurrentView()).getText()).isEqualTo("Connect Twitter");

        TextSwitcher helperText = button.findViewById(R.id.ifttt_helper_text);
        assertThat(helperText.getCurrentView()).isInstanceOf(TextView.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testOwnerServiceCheck() throws IOException {
        Connection connection = loadConnection(getClass().getClassLoader());

        button.setup("a@b.com", "not_owner_service", client, "", () -> "");
        button.setConnection(connection);

        fail();
    }

    @Test
    public void testOnDarkBackground() {
        TextSwitcher helperText = button.findViewById(R.id.ifttt_helper_text);
        TextView currentHelperTextView = (TextView) helperText.getCurrentView();
        TextView nextHelperTextView = (TextView) helperText.getNextView();

        FrameLayout buttonRoot = button.findViewById(R.id.ifttt_button_root);

        button.setOnDarkBackground(true);
        assertThat(currentHelperTextView.getCurrentTextColor()).isEqualTo(
                ContextCompat.getColor(button.getContext(), R.color.ifttt_footer_text_white));
        assertThat(nextHelperTextView.getCurrentTextColor()).isEqualTo(
                ContextCompat.getColor(button.getContext(), R.color.ifttt_footer_text_white));
        assertThat(buttonRoot.getForeground()).isNotNull();

        button.setOnDarkBackground(false);
        assertThat(currentHelperTextView.getCurrentTextColor()).isEqualTo(
                ContextCompat.getColor(button.getContext(), R.color.ifttt_footer_text_black));
        assertThat(nextHelperTextView.getCurrentTextColor()).isEqualTo(
                ContextCompat.getColor(button.getContext(), R.color.ifttt_footer_text_black));
        assertThat(buttonRoot.getForeground()).isNull();
    }

    @Test
    public void testDispatchStates() throws IOException {
        button.setup("a@b.com", "instagram", client, "", () -> "");

        AtomicReference<ButtonState> currentStateRef = new AtomicReference<>(ButtonState.Initial);
        AtomicReference<ButtonState> prevStateRef = new AtomicReference<>();
        AtomicReference<ErrorResponse> errorRef = new AtomicReference<>();
        button.setButtonStateChangeListener(new ButtonStateChangeListener() {
            @Override
            public void onStateChanged(ButtonState currentState, ButtonState previousState) {
                currentStateRef.set(currentState);
                prevStateRef.set(previousState);
            }

            @Override
            public void onError(ErrorResponse errorResponse) {
                errorRef.set(errorResponse);
            }
        });

        Connection connection = loadConnection(getClass().getClassLoader());
        button.setConnection(connection);
        assertThat(currentStateRef.get()).isEqualTo(ButtonState.Initial);

        button.setConnectResult(
                new ConnectResult(ConnectResult.NextStep.ServiceAuthentication, null, "instagram", null));
        assertThat(currentStateRef.get()).isEqualTo(ButtonState.ServiceAuthentication);

        button.setConnectResult(new ConnectResult(ConnectResult.NextStep.Error, null, null, "error"));
        assertThat(currentStateRef.get()).isEqualTo(ButtonState.Initial);
        assertThat(errorRef.get()).isNotNull();

        errorRef.set(null);
        button.setConnectResult(new ConnectResult(ConnectResult.NextStep.Complete, "token", null, null));
        assertThat(currentStateRef.get()).isEqualTo(ButtonState.Enabled);
    }
}