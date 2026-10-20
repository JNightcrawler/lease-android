package com.billingapp.android.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A single background thread for all Room writes (inserts/updates/deletes), plus a
 * way to hop back to the main thread for UI callbacks. Room's LiveData query results
 * are already delivered on the main thread automatically, so this is only needed for
 * write operations and one-off synchronous reads.
 */
public final class AppExecutors {

    private static volatile AppExecutors instance;

    private final ExecutorService diskIO = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    public void diskIO(Runnable runnable) {
        diskIO.execute(runnable);
    }

    public void mainThread(Runnable runnable) {
        mainThread.post(runnable);
    }
}
