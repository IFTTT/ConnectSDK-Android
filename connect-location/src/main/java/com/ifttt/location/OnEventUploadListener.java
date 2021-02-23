package com.ifttt.location;

import android.content.Context;

/**
 * A listener interface for geo-fence uploads from {@link ConnectLocation#reportEvent(Context, double, double, OnEventUploadListener)}
 * that status of any given method call. You can use this interface to listen to whether a reported event was uploaded
 * or skipped.
 */
public interface OnEventUploadListener {

    void onUploadEvent(String fenceKey, LocationEventUploader.EventType eventType);

    void onUploadSkipped(String fenceKey, String reason);
}
