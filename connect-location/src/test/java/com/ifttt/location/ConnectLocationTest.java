package com.ifttt.location;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ifttt.connect.ui.ConnectButton;
import com.ifttt.connect.ui.TestActivity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = 28)
public class ConnectLocationTest {

    private ConnectButton button;

    @Before
    public void setUp(){
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);
        scenario.onActivity(activity -> {
            button = activity.findViewById(R.id.ifttt_connect_button_test);
        });
    }

    @Test
    public void setUpWithoutInit() {
        ConnectLocation.setUpWithConnectButton(button);
        fail();
    }
}
