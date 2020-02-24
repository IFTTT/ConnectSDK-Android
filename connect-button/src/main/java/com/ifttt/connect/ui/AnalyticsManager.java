package com.ifttt.connect.ui;

import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.ifttt.connect.BuildConfig;
import com.ifttt.connect.analytics.tape.ObjectQueue;
import com.ifttt.connect.analytics.tape.QueueFile;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;

/*
 * This is the main class implementing analytics for the ConnectButton SDK. It is responsible for :
 * 1. Providing different analytics tracking methods
 * 2. A Queue for storing the events, including synchronous add, read and remove operations.
 * 3. Setting up the WorkManager with a one time work request to schedule event uploads to server.
 * Events are scheduled to be uploaded for every 5 events or when submitFlush is explicitly called by the ConnectButton class
 * */
final class AnalyticsManager {

    private static AnalyticsManager INSTANCE = null;

    private ObjectQueue<AnalyticsEventPayload> queue;

    private final WorkManager workManager;

    private static boolean analyticsDisabled = false;

    /*
     * This lock is to ensure that only one queue operation - add, remove, or peek can be performed at a time.
     * This will help make sure that items are not removed while we are adding them to the queue.
     * */
    private final Object queueLock = new Object();
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int FLUSH_QUEUE_SIZE = 5;

    private static final String WORK_ID_QUEUE_FLUSH = "analytics_queue_flush";
    private static final String QUEUE_FILE_NAME = "analytics-queue-file";

    private AnalyticsManager(Context context) {
        /*
         * Try to create a file based queue to store the analytics event
         * Use the in-memory queue as fallback
         **/
        Moshi moshi = new Moshi.Builder().build();

        try {
            File folder = context.getDir("analytics-disk-queue", Context.MODE_PRIVATE);
            QueueFile queueFile = createQueueFile(folder);
            queue = ObjectQueue.create(queueFile, new AnalyticsPayloadConverter(moshi));
        } catch (IOException e) {
            queue = ObjectQueue.createInMemory();
        }

        workManager = WorkManager.getInstance(context);
    }

    static synchronized AnalyticsManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AnalyticsManager(context.getApplicationContext());
        }
        return INSTANCE;
    }

    void disableTracking() {
        analyticsDisabled = true;
    }

    /*
     * This method generates a UI item impression event
     * */
    @MainThread
    void trackUiImpression(AnalyticsObject obj, AnalyticsLocation location) {
        trackItemEvent("sdk.impression", obj, location);
    }

    /*
     * This method generates a UI item click event
     * */
    @MainThread
    void trackUiClick(AnalyticsObject obj, AnalyticsLocation location) {
        trackItemEvent("sdk.click", obj, location);
    }

    /*
     * Process the event data before adding it to the event queue
     * */
    private void trackItemEvent(String name, AnalyticsObject obj, AnalyticsLocation location) {
        if (analyticsDisabled) {
            return;
        }

        Map<String, String> properties = new HashMap<>();

        properties.put("object_id", obj.id);
        properties.put("object_type", obj.type);
        if (obj instanceof AnalyticsObject.ConnectionAnalyticsObject) {
            properties.put("object_status",
                    ((AnalyticsObject.ConnectionAnalyticsObject) obj).status);
        }

        properties.put("location_id", location.id);
        properties.put("location_type", location.type);

        properties.put("sdk_version", BuildConfig.VERSION_NAME);
        String timestamp = Long.toString(System.currentTimeMillis());

        performEnqueue(new AnalyticsEventPayload(name, timestamp, properties));
    }

    /*
     * Enqueue an async task to add to the queue.
     * The default serial scheduler of AsyncTask will be used to ensure that events are enqueued in the correct order.
     **/
    private void performEnqueue(AnalyticsEventPayload payload) {
        new EnqueueTask(this).execute(payload);
    }

    /*
     * Adds an item to the queue
     * */

    @VisibleForTesting
    @WorkerThread
    void performAdd(AnalyticsEventPayload payload) {
        if (queue.size() >= MAX_QUEUE_SIZE) {
            try {
                synchronized (queueLock) {
                    /*
                     * Double check the lock.
                     * The payload may have been uploaded and removed from the queue while we were waiting.
                     * */
                    if (queue.size() >= MAX_QUEUE_SIZE) {
                        /*
                         * Remove the oldest payload to accommodate the new one
                         * */
                        queue.remove(1);
                    }
                }
            } catch (IOException e) {
                return;
            }
        }

        try {
            synchronized (queueLock) {
                queue.add(payload);
            }

            if (queue.size() >= FLUSH_QUEUE_SIZE) {
                flushTrackedEvents();
            }
        } catch (IOException e) {
            return;
        }
    }

    /*
     * Removes n items from the queue
     * */
    @WorkerThread
    void performRemove(int n) {
        try {
            synchronized (queueLock) {
                queue.remove(n);
            }
        } catch (IOException e) {
            return;
        }
    }

    /*
    * This method will flush the analytics event queue
    * Call this when
    * 1. The queue size reaches a set threshold,
    * 2. BaseConnectButton is attached to window
    * 3. BaseConnectButton is detached from window
    * */
    void flushTrackedEvents() {
        OneTimeWorkRequest oneTimeWorkRequest =
                new OneTimeWorkRequest.Builder(AnalyticsEventUploader.class).build();
        workManager.enqueueUniqueWork(WORK_ID_QUEUE_FLUSH, ExistingWorkPolicy.KEEP,
                oneTimeWorkRequest);
    }

    /*
    * This will only be used for testing purpose to clear the queue between tests
    * */
    @VisibleForTesting
    void clearQueue() {
        try {
            queue.clear();
        } catch(IOException e) {
        }
    }

    /*
     * Reads n items from the queue
     * */
    @WorkerThread
    @Nullable
    List<AnalyticsEventPayload> performRead() {
        try {
            synchronized (queueLock) {
                return queue.peek(queue.size());
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static QueueFile createQueueFile(File folder) throws IOException {
        if (!(folder.exists() || folder.mkdirs() || folder.isDirectory())) {
            throw new IOException("Could not create directory at " + folder);
        }

        File file = new File(folder, QUEUE_FILE_NAME);
        try {
            return new QueueFile.Builder(file).build();
        } catch (IOException e) {
            if (file.delete()) {
                return new QueueFile.Builder(file).build();
            } else {
                throw new IOException(
                        "Could not create queue file (" + QUEUE_FILE_NAME + ") in " + folder + ".");
            }
        }
    }

    final static class EnqueueTask extends AsyncTask<AnalyticsEventPayload, Void, Void> {

        private final AnalyticsManager analyticsManager;

        EnqueueTask(AnalyticsManager analyticsManager) {
            this.analyticsManager = analyticsManager;
        }

        @Override
        protected final Void doInBackground(AnalyticsEventPayload... items) {
            analyticsManager.performAdd(items[0]);
            return null;
        }
    }

    /** Converter which uses Moshi to serialize instances of class AnalyticsEventPayload to disk. */
    private static final class AnalyticsPayloadConverter
            implements ObjectQueue.Converter<AnalyticsEventPayload> {
        private final JsonAdapter<AnalyticsEventPayload> jsonAdapter;

        AnalyticsPayloadConverter(Moshi moshi) {
            this.jsonAdapter = moshi.adapter(AnalyticsEventPayload.class);
        }

        @Override
        public AnalyticsEventPayload from(byte[] bytes) throws IOException {
            return jsonAdapter.fromJson(new Buffer().write(bytes));
        }

        @Override
        public void toStream(AnalyticsEventPayload val, OutputStream os) throws IOException {
            try (BufferedSink sink = Okio.buffer(Okio.sink(os))) {
                jsonAdapter.toJson(sink, val);
            }
        }
    }
}
