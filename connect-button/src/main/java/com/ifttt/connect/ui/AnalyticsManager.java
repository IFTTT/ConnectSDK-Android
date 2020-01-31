package com.ifttt.connect.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.ifttt.connect.BuildConfig;
import com.ifttt.connect.analytics.tape.ObjectQueue;
import com.ifttt.connect.analytics.tape.QueueFile;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import android.os.Build.VERSION;

/*
 * This is the main class implementing analytics for the ConnectButton SDK. It is responsible for :
 * 1. Providing different analytics tracking methods,
 * 2. A Queue for storing the events, including synchronous add, read and remove operations.
 * 3. Setting up the WorkManager with a one time work request to schedule event uploads to server.
 * Events are scheduled to be uploaded for every 5 events or when submitFlush is explicitly called by the ConnectButton class
 * */
final class AnalyticsManager {

    private Context context;
    private static AnalyticsManager INSTANCE = null;
    private ObjectQueue queue;
    private String previousEventName;
    private String anonymousId;
    /*
     * This lock is to ensure that only one queue operation - add, remove, or peek can be performed at a time.
     * This will help make sure that items are not removed while we are adding them to the queue.
     * */
    private final Object queueLock = new Object();
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int FLUSH_QUEUE_SIZE = 5;

    private static final String WORK_ID_QUEUE_POLLING = "analytics_queue_polling";

    private AnalyticsManager(Context context) {
        this.context = context;
        /*
         * Try to create a file based queue to store the analytics event
         * Use the in-memory queue as fallback
         **/
        Moshi moshi = new Moshi.Builder()
                .build();

        try {
            File folder = context.getDir("analytics-disk-queue", Context.MODE_PRIVATE);
            QueueFile queueFile = createQueueFile(folder);
            queue = ObjectQueue.create(queueFile, new AnalyticsPayloadConverter(moshi));
        } catch (IOException e) {
            queue = ObjectQueue.createInMemory();
        }

        anonymousId = AnalyticsPreferences.getAnonymousId(context);
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

        Map<String, Object> data = new HashMap<>();

        data.put("name", name);
        data.put("object_id", obj.id);
        data.put("object_type", obj.type);

        mapAttributes(data, obj);

        data.put("location_type", location.type);
        data.put("location_id", location.id);
        data.put("source_location_type", sourceLocation.type);
        data.put("source_location_id", context.getPackageName());
        data.put("sdk_version", BuildConfig.VERSION_NAME);
        data.put("system_version", VERSION.SDK_INT);

        data.put("sdk_anonymous_id", anonymousId);
        if (previousEventName != null) {
            data.put("previous_event_name", previousEventName);
        }

        this.previousEventName = name;

        performEnqueue(data);
    }

    /*
     * Map object specific attributes to an appropriate data parameter
     * */
    private void mapAttributes(Map<String, Object> data, AnalyticsObject obj) {
        // TODO: Map attributes depending on AnalyticsObject type
    }

    /*
    * Returns the current size of the queue
    * */
    int getQueueSize() {
        return queue.size();
    }

    /*
     * Checks the queue size, if it is full, remove the oldest item
     * Add an item to the queue
     * After adding the new item, if the queue reaches threshold limit, perform flush
     * */
    private void performEnqueue(Map payload) {
        Log.d("AnalyticsManager", "Inside perform enqueue, payload= " + payload);

        new EnqueueTask().execute(payload);
    }

    /*
    * Schedules a one time work request to read from the queue, make an api call to submit events and remove them from queue
    * */
    void performFlush() {

        Log.d("AnalyticsManager", "Inside perform flush method");
        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(
                AnalyticsEventUploader.class)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork("analytics_events_post", ExistingWorkPolicy.KEEP, oneTimeWorkRequest);
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
    List<Map<String, Object>> performRead(int n) {
        List<Object> listOfOneItem;
        List<Object> list;

        try {
            listOfOneItem = queue.peek(1);
        } catch (IOException e) {
            listOfOneItem = null;
        }

        try {
            list = queue.peek(queue.size());
        } catch (IOException e) {
            list = null;
        }


        Log.d("AnalyticsManager", "peek(1) = " + listOfOneItem  + ", peek(all) = " + list);
        try {
            synchronized (queueLock) {
                return queue.peek(n);
            }
        } catch (IOException e) {
            return null;
        }
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
    private final class AnalyticsPayloadConverter implements ObjectQueue.Converter<Map<String, Object>> {
        private final JsonAdapter<Map<String, Object>> jsonAdapter;

        AnalyticsPayloadConverter(Moshi moshi) {
            Type type = Types.newParameterizedType(Map.class, String.class, Object.class);
            this.jsonAdapter = moshi.adapter(type);
        }

        @Override public Map<String, Object> from(byte[] bytes) throws IOException {
            return jsonAdapter.fromJson(new Buffer().write(bytes));
        }

        @Override public void toStream(Map<String, Object> val, OutputStream os) throws IOException {
            try (BufferedSink sink = Okio.buffer(Okio.sink(os))) {
                jsonAdapter.toJson(sink, val);
            }
        }
    }

    private final class EnqueueTask extends AsyncTask<Map, Void, Void> {

        @Override
        @SuppressWarnings("unchecked")
        protected final Void doInBackground(Map... item) {
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
                    return null;
                }
            }

            try {
                synchronized (queueLock) {
                    queue.add(item);
                    Log.d("AnalyticsManager", "In background, After adding item, queue = " + queue + ", queue size = " + queue.size());
                    if (queue.size() >= FLUSH_QUEUE_SIZE) {
                        performFlush();
                    }
                }
            } catch (IOException e) {
                return null;
            }
            return null;
        }
    }
}
