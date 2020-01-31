package com.ifttt.connect.analytics;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public final class AnalyticsManager {

    private static AnalyticsManager INSTANCE = null;
    private ObjectQueue queue;
    private JsonAdapter eventsSerializer;
    private Context context;
    private Handler handler;


    private String previousEventName;
    private String anonymousId;

    private static final String WORK_ID_QUEUE_POLLING = "analytics_queue_polling";
    private static final int REQUEST_ENQUEUE = 0;
    private static final int REQUEST_FLUSH = 1;

    private AnalyticsManager(Context context, HandlerThread handlerThread) {
        this.context = context;
        /*
         * Try to create a file based queue to store the analytics event
         * Use the in-memory queue as fallback
         **/
        Moshi moshi = new Moshi.Builder()
                .build();
        eventsSerializer = moshi.adapter(Types.newParameterizedType(List.class, Map.class));

        try {
            File folder = context.getDir("analytics-disk-queue", Context.MODE_PRIVATE);
            QueueFile queueFile = createQueueFile(folder);
            queue = ObjectQueue.create(queueFile, new AnalyticsPayloadConverter(moshi));
        } catch (IOException e) {
            queue = ObjectQueue.createInMemory();
        }

        anonymousId = AnalyticsPreferences.getAnonymousId(context);
        this.handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case REQUEST_ENQUEUE:
                        Map<String, Object> payload = (Map<String, Object>) msg.obj;
                        performEnqueue(payload);
                        break;
                    case REQUEST_FLUSH:
                        performFlush();
                        break;
                    default:
                        throw new AssertionError("Unknown dispatcher message: " + msg.what);
                }
            }
        };
    }

    public static AnalyticsManager getInstance(Context context, HandlerThread handlerThread) {
        if (INSTANCE == null) {
            INSTANCE = new AnalyticsManager(context, handlerThread);
        }
        return INSTANCE;
    }

    /*
     * This method generates a UI item impression event
     * */
    public void trackUiImpression(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.impression", obj, location, sourceLocation);
    }

    /*
     * This method generates a UI item click event
     * */
    public void trackUiClick(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.click", obj, location, sourceLocation);
    }

    /*
     * This method generates a system change event
     * */
    public void trackSystemEvent(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.system", obj, location, sourceLocation);
    }

    /*
     * This method generates a state change event for an object
     * */
    public void trackStateChangeEvent(AnalyticsObject obj, AnalyticsLocation location,
            AnalyticsLocation sourceLocation) {
        trackItemEvent("android.statechange", obj, location, sourceLocation);
    }

    /*
     * This method generates screen view event
     * */
    public void trackScreenView(AnalyticsObject obj, AnalyticsLocation location,
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

        handler.sendMessage(handler.obtainMessage(REQUEST_ENQUEUE, data));
    }

    /*
     * Map object specific attributes to an appropriate data parameter
     * */
    private void mapAttributes(Map<String, Object> data, AnalyticsObject obj) {
        // TODO: Map attributes depending on AnalyticsObject type
    }

    /*
     * This lock is to ensure that only one queue operation - add, remove, or peek can be performed at a time.
     * This will help make sure that items are not removed while we are adding them to the queue.
     * */
    private final Object queueLock = new Object();
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int FLUSH_QUEUE_SIZE = 5;

    /*
     * Removes n items from the queue
     * */
    private void performRemove(int n) {
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
    private List<Map<String, Object>> performRead(int n) {
        try {
            synchronized (queueLock) {
                return queue.peek(n);
            }
        } catch (IOException e) {
            return null;
        }
    }

    /*
     * Checks the queue size, if it is full, remove the oldest item
     * Add an item to the queue
     * After adding the new item, if the queue reaches threshold limit, perform flush
     * */
    private void performEnqueue(Map<String, Object> payload) {
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
                    handler.sendMessage(handler.obtainMessage(REQUEST_FLUSH));
                }
            }
        } catch (IOException e) {
            return;
        }
    }

    // Call this from the ConnectButton class to periodically flush out events
    public void  submitFlush() {
        handler.sendMessage(handler.obtainMessage(REQUEST_FLUSH));
    }

    /*
    * Reads from the queue, schedules a one time work request to make an api call, and removes from the queue
    * */
    private void performFlush() {
        // Read from the queue
        int queueSize = queue.size();
        List<Map<String, Object>> list = performRead(queueSize);

        // One time request to send events to server
        Constraints constraints =
                new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();

        String eventsJson = eventsSerializer.toJson(list);

        Data data = new Data.Builder()
                .putString("event_data", eventsJson)
                .build();

        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(AnalyticsEventUploader.class)
                .setInputData(data)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_ID_QUEUE_POLLING, ExistingWorkPolicy.KEEP, oneTimeWorkRequest);

        // Remove data from queue
        performRemove(queueSize);
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
    class AnalyticsPayloadConverter implements ObjectQueue.Converter<Map> {
        private final JsonAdapter<Map> jsonAdapter;

        AnalyticsPayloadConverter(Moshi moshi) {
            this.jsonAdapter = moshi.adapter(Map.class);
        }

        @Override public Map from(byte[] bytes) throws IOException {
            return jsonAdapter.fromJson(new Buffer().write(bytes));
        }

        @Override public void toStream(Map val, OutputStream os) throws IOException {
            try (BufferedSink sink = Okio.buffer(Okio.sink(os))) {
                jsonAdapter.toJson(sink, val);
            }
        }
    }
}
