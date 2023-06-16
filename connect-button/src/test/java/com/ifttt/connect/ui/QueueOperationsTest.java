package com.ifttt.connect.ui;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ifttt.connect.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 28)
public final class QueueOperationsTest {
    private AnalyticsManager analyticsManager;

    @Before
    public void setup() {
        ActivityController<TestActivity> controller = Robolectric.buildActivity(TestActivity.class);
        controller.get().setTheme(R.style.Base_Theme_AppCompat);
        controller.create().start();

        analyticsManager = AnalyticsManager.getInstance(controller.get());
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
