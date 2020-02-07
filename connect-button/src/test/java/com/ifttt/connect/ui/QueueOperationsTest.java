package com.ifttt.connect.ui;

import android.app.Activity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ifttt.connect.ShadowAnimatorSet;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
@Config(shadows = { ShadowAnimatorSet.class })
public final class QueueOperationsTest {

    private final Activity activity = Robolectric.buildActivity(TestActivity.class).create().get();
    private AnalyticsManager analyticsManager;

    @Before
    public void setup() {
        analyticsManager = AnalyticsManager.getInstance(activity);
        analyticsManager.clearQueue();
    }

    @Test
    public void testEnqueueAndRead() {
        assertThat(analyticsManager.performRead().size()).isEqualTo(0);

        analyticsManager.performAdd(new AnalyticsEventPayload("event1", "", new HashMap<>()));
        analyticsManager.performAdd(new AnalyticsEventPayload("event2", "", new HashMap<>()));
        analyticsManager.performAdd(new AnalyticsEventPayload("event3", "", new HashMap<>()));

        assertThat(analyticsManager.performRead().size()).isEqualTo(3);
    }

    @Test
    public void testRemove() {
        assertThat(analyticsManager.performRead().size()).isEqualTo(0);

        analyticsManager.performAdd(new AnalyticsEventPayload("event1", "", new HashMap<>()));
        analyticsManager.performAdd(new AnalyticsEventPayload("event2", "", new HashMap<>()));
        analyticsManager.performAdd(new AnalyticsEventPayload("event3", "", new HashMap<>()));

        analyticsManager.performRemove(3);
        assertThat(analyticsManager.performRead().size()).isEqualTo(0);
    }
}
