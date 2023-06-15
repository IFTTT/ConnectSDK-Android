package com.ifttt.location;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.Configuration;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public final class ConnectionRefresherTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        Configuration config = new Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(new SynchronousExecutor())
            .build();

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);
    }

    @Test
    public void shouldAddTagToPeriodicJob() throws ExecutionException, InterruptedException {
        ConnectionRefresher.schedule(context, "ConnectionID");

        WorkManager workManager = WorkManager.getInstance(context);
        List<WorkInfo> infoList = workManager.getWorkInfosForUniqueWork(ConnectionRefresher.WORK_ID_CONNECTION_POLLING)
            .get();

        assertThat(infoList).hasSize(1);
        assertThat(infoList.get(0).getState()).isEqualTo(WorkInfo.State.ENQUEUED);
        assertThat(infoList.get(0).getTags()).contains("connection_id:ConnectionID");
    }

    @Test
    public void shouldScheduleExecuteJobIfPeriodicIsScheduled() throws ExecutionException, InterruptedException {
        WorkManager workManager = WorkManager.getInstance(context);

        ConnectionRefresher.schedule(context, "ConnectionID");
        List<WorkInfo> infoList = workManager.getWorkInfosForUniqueWork(ConnectionRefresher.WORK_ID_CONNECTION_POLLING)
            .get();

        UUID id = ConnectionRefresher.scheduleOneTimeRefreshWork(workManager, infoList);
        assertThat(id).isNotNull();

        WorkInfo info = workManager.getWorkInfoById(id).get();
        assertThat(info.getState()).isEqualTo(WorkInfo.State.ENQUEUED);
    }

    @Test
    public void shouldNotScheduledWithCancelledPeriodicJob() throws ExecutionException, InterruptedException {
        WorkManager workManager = WorkManager.getInstance(context);

        ConnectionRefresher.schedule(context, "ConnectionID");
        ConnectionRefresher.cancel(context);

        List<WorkInfo> infoList = workManager.getWorkInfosForUniqueWork(ConnectionRefresher.WORK_ID_CONNECTION_POLLING)
            .get();

        UUID id = ConnectionRefresher.scheduleOneTimeRefreshWork(workManager, infoList);
        assertThat(id).isNull();
    }

    @Test
    public void shouldNotScheduledForIfNoPeriodicJob() {
        WorkManager workManager = WorkManager.getInstance(context);
        UUID id = ConnectionRefresher.scheduleOneTimeRefreshWork(workManager, Collections.emptyList());
        assertThat(id).isNull();
    }
}
