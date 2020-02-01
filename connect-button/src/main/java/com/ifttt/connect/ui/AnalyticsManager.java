package com.ifttt.connect.ui;

import android.content.Context;
import android.os.AsyncTask;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.ifttt.connect.BuildConfig;
import com.ifttt.connect.analytics.tape.ObjectQueue;
import com.ifttt.connect.analytics.tape.QueueFile;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import android.os.Build.VERSION;

/*
 * This is the main class implementing analytics for the ConnectButton SDK. It is responsible for :
 * 1. Providing different analytics tracking methods
 * 2. A Queue for storing the events, including synchronous add, read and remove operations.
 * 3. Setting up the WorkManager with a one time work request to schedule event uploads to server.
 * Events are scheduled to be uploaded for every 5 events or when submitFlush is explicitly called by the ConnectButton class
 * */
final class AnalyticsManager {

    private static AnalyticsManager INSTANCE = null;
    private ObjectQueue queue;
    private static String packageName;
    private static WorkManager workManager;

    /*
     * This lock is to ensure that only one queue operation - add, remove, or peek can be performed at a time.
     * This will help make sure that items are not removed while we are adding them to the queue.
     * */
    private final Object queueLock = new Object();
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int FLUSH_QUEUE_SIZE = 5;

    private static final String WORK_ID_QUEUE_POLLING = "analytics_queue_polling";
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
        packageName = context.getPackageName();
    }

    static AnalyticsManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AnalyticsManager(context);
        }
        return INSTANCE;
    }

    /*
     * This method generates a UI item impression event
     * */
    void trackUiImpression(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.impression", obj, location, sourceLocation);
    }

    /*
     * This method generates a UI item click event
     * */
    void trackUiClick(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.click", obj, location, sourceLocation);
    }

    /*
     * This method generates a system change event
     * */
    void trackSystemEvent(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.system", obj, location, sourceLocation);
    }

    /*
     * This method generates a state change event for an object
     * */
    void trackStateChangeEvent(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.statechange", obj, location, sourceLocation);
    }

    /*
     * This method generates screen view event
     * */
    void trackScreenView(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.pageviewed", obj, location, sourceLocation);
    }

    /*
     * Process the event data before adding it to the event queue
     * */
    private void trackItemEvent(String name, AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {

        Map<String, String> data = new HashMap<>();

        data.put("name", name);
        data.put("object_id", obj.id);
        data.put("object_type", obj.type);

        mapAttributes(data, obj);

        data.put("location_type", location.type);
        data.put("location_id", location.id);
        data.put("source_location_type", sourceLocation.type);
        data.put("source_location_id", packageName);
        data.put("sdk_version", BuildConfig.VERSION_NAME);
        data.put("system_version", Integer.toString(VERSION.SDK_INT));

        //TODO: Format timestamp
        data.put("timestamp", Long.toString(System.currentTimeMillis()));

        performEnqueue(data);
    }

    /*
     * Map object specific attributes to an appropriate data parameter
     * */
    private void mapAttributes(Map<String, String> data, AnalyticsObject obj) {
        // TODO: Map attributes depending on AnalyticsObject type
    }

    /*
     * Returns the current size of the queue
     * */
    int getQueueSize() {
        return queue.size();
    }

    /*
     * Enqueue an async task to add to the queue.
     * The default serial scheduler of AsyncTask will be used to ensure that events are enqueued in the correct order.
     **/
    private void performEnqueue(Map payload) {
        new EnqueueTask(this).execute(payload);
    }

    /*
     * Schedules a one time work request to read from the queue, make an api call to submit events and remove them from queue
     * */
    static void performFlush() {
        OneTimeWorkRequest oneTimeWorkRequest =
                new OneTimeWorkRequest.Builder(AnalyticsEventUploader.class).build();
        workManager.enqueueUniqueWork(WORK_ID_QUEUE_POLLING, ExistingWorkPolicy.KEEP,
                oneTimeWorkRequest);
    }

    /*
     * Removes n items from the queue
     * */
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
     * Reads n items from the queue
     * */
    @SuppressWarnings("unchecked")
    List<Map<String, String>> performRead(int n) {
        try {
            synchronized (queueLock) {
                return queue.peek(n);
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

    final static class EnqueueTask extends AsyncTask<Map, Void, Void> {

        private final AnalyticsManager analyticsManager;

        EnqueueTask(AnalyticsManager analyticsManager) {
            this.analyticsManager = analyticsManager;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected final Void doInBackground(Map... items) {
            ObjectQueue queue = analyticsManager.queue;

            if (queue.size() >= MAX_QUEUE_SIZE) {
                try {
                    synchronized (analyticsManager.queueLock) {
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
                    return null;
                }
            }

            try {
                synchronized (analyticsManager.queueLock) {
                    queue.add(items[0]);
                }

                if (queue.size() >= FLUSH_QUEUE_SIZE) {
                    performFlush();
                }
            } catch (IOException e) {
                return null;
            }
            return null;
        }
    }

    /** Converter which uses Moshi to serialize instances of class Map<String, String> to disk. */
    private final class AnalyticsPayloadConverter
            implements ObjectQueue.Converter<Map<String, String>> {
        private final JsonAdapter<Map<String, String>> jsonAdapter;

        AnalyticsPayloadConverter(Moshi moshi) {
            Type type = Types.newParameterizedType(Map.class, String.class, String.class);
            this.jsonAdapter = moshi.adapter(type);
        }

        @Override
        public Map<String, String> from(byte[] bytes) throws IOException {
            return jsonAdapter.fromJson(new Buffer().write(bytes));
        }

        @Override
        public void toStream(Map<String, String> val, OutputStream os) throws IOException {
            try (BufferedSink sink = Okio.buffer(Okio.sink(os))) {
                jsonAdapter.toJson(sink, val);
            }
        }
    }
}
