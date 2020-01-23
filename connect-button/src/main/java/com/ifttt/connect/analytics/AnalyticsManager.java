package com.ifttt.connect.analytics;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.ifttt.connect.analytics.tape.ObjectQueue;
import com.ifttt.connect.analytics.tape.QueueFile;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;

/*
 * This is the main class implementing analytics for the ConnectButton SDK. It is responsible for :
 * 1. Providing different analytics tracking methods,
 * 2. A Queue for storing the events, including synchronous add, read and remove operations.
 * 3. Setting up the WorkManager with a one time work request to schedule event uploads to server.
 * Events are scheduled to be uploaded for every 5 events or when performFlush is explicitly called.
 * */
public class AnalyticsManager {

    private static AnalyticsManager instance = null;
    private ObjectQueue queue;
    private Context context;

    private String previousEventName;
    private String anonymousId;

    private static final String WORK_ID_QUEUE_POLLING = "analytics_queue_polling";

    private AnalyticsManager(Context context) {
        this.context = context;
        /*
         * Try to create a file based queue to store the analytics event
         * Use the in-memory queue as fallback
         **/
        try {
            File folder = context.getDir("analytics-disk-queue", Context.MODE_PRIVATE);
            QueueFile queueFile = createQueueFile(folder);
            Moshi moshi = new Moshi.Builder().build();
            queue = ObjectQueue.create(queueFile, new AnalyticsEventConverter<>(moshi));
        } catch (IOException e) {
            queue = ObjectQueue.createInMemory();
        }
        anonymousId = AnalyticsPreferences.getAnonymousId(context);
    }

    static AnalyticsManager getInstance(Context context) {
        if (instance == null) {
            instance = new AnalyticsManager(context);
        }

        return instance;
    }

    /*
     * This method tracks a UI element impression, call it to track impressions on buttons, texts and so on.
     * */
    public void trackUiImpression(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.impression", obj, location, sourceLocation);
    }

    /*
     * This method tracks a UI element click, call it to track clicks on buttons, texts and so on.
     * */
    public void trackUiClick(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.click", obj, location, sourceLocation);
    }

    /*
     * This method tracks a system event,
     * call it to track events where some important change occured and it cannot be categorized as a state change event for a particular object
     * */
    public void trackSystemEvent(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.system", obj, location, sourceLocation);
    }

    /*
     * This method tracks a state change event, call it to track events where a state was changed for an object
     * */
    public void trackStateChangeEvent(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.statechange", obj, location, sourceLocation);
    }

    /*
     * This method tracks screen views, call it when the activity with the Connect button is in foreground
     * */
    public void trackScreenView(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.pageviewed", obj, location, sourceLocation);
    }

    /*
     * Process the event data before adding it to the queue
     * */
    private void trackItemEvent(String name, AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();

        data.put("object_id", obj.id);
        data.put("object_type", obj.type);

        // Special attributes that only certain types of objects have.
        mapAttributes(data, obj);

        data.put("location_type", location.type);
        data.put("location_id", location.id);
        data.put("source_location_type", sourceLocation.type);

        // TODO: Add source_location_id = {app_bundle_id, package_name} to data
        // TODO: Add sdk version code + name, android version

        data.put("sdk_anonymous_id", anonymousId);
        if (previousEventName != null) {
            data.put("previous_event_name", previousEventName);
        }

        this.previousEventName = name;

        performEnqueue(new AnalyticsEventPayload(name, data));
    }

    /*
     * Map object specific attributes to an appropriate data parameter
     * */
    private void mapAttributes(LinkedHashMap<String, Object> data, AnalyticsObject obj) {
        // TODO: Map attributes depending on AnalyticsObject type
    }

    /*
     * If adding to queue and removing from queue operations are not synchronized using a lock,
     * we may end up with a case where we are in the middle of adding to the queue, but the repeat interval for the periodic work has elapsed,
     * which invokes reading all elements from the queue, followed by removing.
     * This lock ensures that the payloads are not removed while we are adding them.
     * */
    private final Object queueLock = new Object();
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int FLUSH_QUEUE_SIZE = 5;

    int getCurrentQueueSize() {
        return queue.size();
    }

    void performRemove(int count) {
        try {
            synchronized (queueLock) {
                queue.remove(count);
            }
        } catch (IOException e) {
            // TODO: Exception handling
        }
    }

    List<AnalyticsEventPayload> performRead(int count) {
        try {
            synchronized (queueLock) {
                return queue.peek(count);
            }
        } catch (IOException e) {
            // TODO: Exception handling
            return null;
        }
    }

    private void performEnqueue(AnalyticsEventPayload payload) {

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
                if (queue.size() >= FLUSH_QUEUE_SIZE) {
                    performFlush();
                }
            }
        } catch (IOException e) {
            // TODO: Exception handling
        }
    }

    /*
    * Call this method when the ConnectButton is initialized to flush the initial set of events
    * */
    void performFlush() {
        // One time request to read the queue, send events to server and flush the queue.
        Constraints constraints =
                new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();

        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(AnalyticsEventUploader.class).setConstraints(constraints).build();
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_ID_QUEUE_POLLING, ExistingWorkPolicy.KEEP, oneTimeWorkRequest);
    }

    private static QueueFile createQueueFile(File folder) throws IOException {
        String name = "analytics-queue-file";

        if (!(folder.exists() || folder.mkdirs() || folder.isDirectory())) {
            throw new IOException("Could not create directory at " + folder);
        }

        File file = new File(folder, name);
        try {
            return new QueueFile.Builder(file).build();
        } catch (IOException e) {
            if (file.delete()) {
                return new QueueFile.Builder(file).build();
            } else {
                throw new IOException(
                        "Could not create queue file (" + name + ") in " + folder + ".");
            }
        }
    }

    /** Converter which uses Moshi to serialize instances of class T to disk. */
    class AnalyticsEventConverter<T>
            implements ObjectQueue.Converter<AnalyticsEventPayload> {
        private final JsonAdapter<com.ifttt.connect.analytics.AnalyticsEventPayload> jsonAdapter;

        public AnalyticsEventConverter(Moshi moshi) {
            this.jsonAdapter = moshi.adapter(com.ifttt.connect.analytics.AnalyticsEventPayload.class);
        }

        @Override
        public com.ifttt.connect.analytics.AnalyticsEventPayload from(byte[] bytes) throws IOException {
            return jsonAdapter.fromJson(new Buffer().write(bytes));
        }

        @Override
        public void toStream(com.ifttt.connect.analytics.AnalyticsEventPayload val, OutputStream os) throws IOException {
            try (BufferedSink sink = Okio.buffer(Okio.sink(os))) {
                jsonAdapter.toJson(sink, val);
            }
        }
    }
}
