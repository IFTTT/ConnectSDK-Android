package com.ifttt.connect.ui;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import com.ifttt.connect.api.PendingResult;

final class PendingResultLifecycleObserver<T> implements LifecycleObserver {

    private final PendingResult<T> pendingResult;

    PendingResultLifecycleObserver(PendingResult<T> pendingResult) {
        this.pendingResult = pendingResult;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        pendingResult.cancel();
    }
}
