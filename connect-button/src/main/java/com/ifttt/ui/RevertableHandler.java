package com.ifttt.ui;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Handler} wrapper that works with {@link Revertable} to automatically schedule a revert action for any
 * running Revertable.
 */
final class RevertableHandler {

    private final List<Runnable> revertables = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Run a {@link Revertable}, and automatically schedule a Runnable to call {@link Revertable#revert()} after a
     * delay.
     *
     * @param revertable Revertable object to run.
     * @param delay Delay in milliseconds that the revert function will be called.
     */
    void run(Revertable revertable, long delay) {
        Runnable toRevert = new Runnable() {
            @Override
            public void run() {
                revertables.remove(this);
                revertable.revert();
            }
        };
        revertables.add(toRevert);

        revertable.run();
        handler.postDelayed(toRevert, delay);
    }

    /**
     * Revert all of the scheduled revert actions.
     */
    void revertAll() {
        for (Runnable revertable : revertables) {
            handler.removeCallbacks(revertable);
            revertable.run();
        }

        revertables.clear();
    }

    /**
     * Remove all of the scheduled revert actions.
     */
    void clear() {
        for (Runnable revertable : revertables) {
            handler.removeCallbacks(revertable);
        }

        revertables.clear();
    }
}
