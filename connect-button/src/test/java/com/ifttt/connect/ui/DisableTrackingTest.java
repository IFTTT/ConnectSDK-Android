package com.ifttt.connect.ui;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.Configuration;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@RunWith(AndroidJUnit4.class)
@LooperMode(LooperMode.Mode.PAUSED)
public final class DisableTrackingTest {

    private AnalyticsManager analyticsManager;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        Configuration config = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);

        analyticsManager = AnalyticsManager.getInstance(context);
    }

    @Test
    public void testAnalyticsDisabled() {
        analyticsManager.clearQueue();
        assertThat(analyticsManager.performRead().size()).isEqualTo(0);

        analyticsManager.disableTracking();
        analyticsManager.trackUiClick(new AnalyticsObject("obj_id", "obj_type"), new AnalyticsLocation("loc_id", "loc_type"));

        assertThat(analyticsManager.performRead().size()).isEqualTo(0);
    }
}
