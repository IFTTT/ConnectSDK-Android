package com.ifttt.connect.analytics;

import android.content.Context;
import com.ifttt.connect.analytics.tape.ObjectQueue;
import com.ifttt.connect.analytics.tape.QueueFile;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;

// Consider making this a builder pattern
class AnalyticsSender {

    /* Singleton instance */
    private static AnalyticsSender instance = null;

    private AnalyticsSender(Context context, String tag) {
        /*
        * Try to create a file based queue to store the analytics event
        * Use the in-memory queue as fallback
        *  */
        try {
            File folder = context.getDir("analytics-disk-queue", Context.MODE_PRIVATE);
            QueueFile queueFile = createQueueFile(folder, tag);
            queue = ObjectQueue.create(queueFile, new AnalyticsEventConverter<>());
        } catch (IOException e) {
            queue = ObjectQueue.createInMemory();
        }
    }

    /*
    * Use this function to get an instance of this Singleton class
    * */
    static AnalyticsSender getInstance(Context context, String tag) {
        if (instance == null) {
            instance = new AnalyticsSender(context, tag);
        }
        return instance;
    }

    private ObjectQueue queue;
    private final Object flushLock = new Object();
    private static final int DEFAULT_FLUSH_INTERVAL = 30 * 1000; // 30s
    private static final int DEFAULT_FLUSH_QUEUE_SIZE = 10;
    private static final int MAX_QUEUE_SIZE = 1000;


    void enqueue(String name, LinkedHashMap<String, Object> data) {
        // TODO: Convert the analytics data to AnalyticsEventPayload before calling performEnqueue
    }


    private void performEnqueue(AnalyticsEventPayload payload) {

        if (queue.size() >= MAX_QUEUE_SIZE) {
            try {
                synchronized (flushLock) {
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
            queue.add(payload);
        } catch (IOException e) {
            return;
        }

        // TODO: Considering the sdk does not generate lot of events, we may want to reduce this number or use a regular flush interval
        if (queue.size() >= DEFAULT_FLUSH_QUEUE_SIZE) {
            performFlush();
        }

    }

    /** Upload payloads to our servers and remove them from the queue file. */
    private void performFlush() {
        // Loop through payload queue using iterator and peek, generate a json formatted batched payload and issue one time work request to Work Manager
        // Remove the items from payload queue using remove after successfully uploading
    }


    private static QueueFile createQueueFile(File folder, String name) throws IOException {
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
                throw new IOException("Could not create queue file (" + name + ") in " + folder + ".");
            }
        }
    }

    /** Converter which uses Moshi to serialize instances of class T to disk. */
    class AnalyticsEventConverter<T> implements ObjectQueue.Converter<T> {

        @Override public T from(byte[] bytes) throws IOException {
            // TODO
            return null;
        }

        @Override public void toStream(T val, OutputStream os) throws IOException {
            // TODO
        }
    }
}
