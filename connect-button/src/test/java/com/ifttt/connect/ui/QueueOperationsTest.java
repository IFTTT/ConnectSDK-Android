package com.ifttt.connect.ui;

import android.app.Activity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ifttt.connect.ShadowAnimatorSet;
import java.io.IOException;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
@Config(shadows = { ShadowAnimatorSet.class })
public class QueueOperationsTest {

    private final Activity activity = Robolectric.buildActivity(TestActivity.class).create().get();
    private AnalyticsManager analyticsManager;

    @Before
    public void setup() {
        analyticsManager = AnalyticsManager.getInstance(activity);

        try {
            analyticsManager.queue.clear();
        } catch (IOException e) {

        }

        AnalyticsManager.destroy();
    }

    @Test
    public void testEnqueueAndRead() {
        assertThat(analyticsManager.queue.size()).isEqualTo(0);

        analyticsManager.performEnqueue(new AnalyticsEventPayload("event1", "", new HashMap<>()));
        analyticsManager.performEnqueue(new AnalyticsEventPayload("event2", "", new HashMap<>()));
        analyticsManager.performEnqueue(new AnalyticsEventPayload("event3", "", new HashMap<>()));

        assertThat(analyticsManager.queue.size()).isEqualTo(3);
    }

    @Test
    public void testRead() {
        assertThat(analyticsManager.queue.size()).isEqualTo(0);

        analyticsManager.performEnqueue(new AnalyticsEventPayload("event1", "", new HashMap<>()));
        analyticsManager.performEnqueue(new AnalyticsEventPayload("event2", "", new HashMap<>()));
        analyticsManager.performEnqueue(new AnalyticsEventPayload("event3", "", new HashMap<>()));

        assertThat(analyticsManager.performRead().size()).isEqualTo(3);
    }

    @Test
    public void testRemove() {
        assertThat(analyticsManager.queue.size()).isEqualTo(0);

        analyticsManager.performEnqueue(new AnalyticsEventPayload("event1", "", new HashMap<>()));
        analyticsManager.performEnqueue(new AnalyticsEventPayload("event2", "", new HashMap<>()));
        analyticsManager.performEnqueue(new AnalyticsEventPayload("event3", "", new HashMap<>()));

        analyticsManager.performRemove(3);
        assertThat(analyticsManager.queue.size()).isEqualTo(0);
    }
}
