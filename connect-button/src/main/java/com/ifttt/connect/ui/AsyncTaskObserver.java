package com.ifttt.connect.ui;

import android.os.AsyncTask;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

final class AsyncTaskObserver implements LifecycleObserver {

    private final AsyncTask task;

    AsyncTaskObserver(AsyncTask task) {
        this.task = task;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void onStop() {
        task.cancel(true);
    }
}
