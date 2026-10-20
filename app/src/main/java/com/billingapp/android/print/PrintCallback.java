package com.billingapp.android.print;

/** Result callback for a print attempt, delivered on the main thread. */
public interface PrintCallback {
    void onSuccess();
    void onError(String message);
}
