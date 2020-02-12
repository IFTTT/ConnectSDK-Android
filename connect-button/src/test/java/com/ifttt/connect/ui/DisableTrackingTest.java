package com.ifttt.connect.ui;

import android.app.Activity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ifttt.connect.ShadowAnimatorSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
@Config(shadows = { ShadowAnimatorSet.class })
public final class DisableTrackingTest {

    private final Activity activity = Robolectric.buildActivity(TestActivity.class).create().get();
    private AnalyticsManager analyticsManager;

    @Before
    public void setup() {
        analyticsManager = AnalyticsManager.getInstance(activity);
    }

    @Test
    public void testAnalyticsDisabled() {
        assertThat(analyticsManager.performRead().size()).isEqualTo(0);

        analyticsManager.disableTracking();
        analyticsManager.trackUiClick(new AnalyticsObject("obj_id", "obj_type"), new AnalyticsLocation("loc_id", "loc_type"));

        assertThat(analyticsManager.performRead().size()).isEqualTo(0);
    }
}
